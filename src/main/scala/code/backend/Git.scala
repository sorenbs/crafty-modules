package code.backend

import org.eclipse.jgit._
import com.yahoo.platform.yui.compressor.JavaScriptCompressor._
import storage.file.FileRepositoryBuilder
import java.io.File
import org.apache.commons.io.FileUtils
import net.liftweb.json._
import com.yahoo.platform.yui.compressor.JavaScriptCompressor
import java.io.InputStream
import java.io.ByteArrayInputStream
import org.mozilla.javascript.{ErrorReporter, EvaluatorException}
import java.io.StringReader
import java.io.OutputStreamWriter
import org.apache.commons.io.output.ByteArrayOutputStream
import org.jets3t.service._
import net.iharder
import org.jets3t.service.security.AWSCredentials
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.S3Object
import com.foursquare.rogue.Rogue._
import net.liftweb.util.Props
import org.apache.commons.io.input.BOMInputStream
import java.io.FileInputStream
import org.apache.commons.io.IOUtils
import net.liftweb.common.Box
import net.liftweb.common.Full
import net.liftweb.common.Failure
import net.liftweb.common.Empty

case class Person(name:String, url:Option[String], email:Option[String])
case class License(`type`:String, url:Option[String])
case class packageJSON(
    files: List[String], 
    version:String, 
    name:String, 
    title:Option[String], 
    description:Option[String],
    jsfiddle:Option[String],
    author:Option[Person], 
    contributors:List[Person], 
    licenses:List[License], 
    keywords: List[String], 
    dependencies:Option[Map[String,String]],
    homepage:Option[String])

object Git {
	implicit val formats = DefaultFormats // Brings in default date formats etc.
	
	def fetch(uri:String) = {
	  var status = "OK"
	  try {
		  // Delete old and download new
		  val dir = "repos/" + uri.split("/").last
		  FileUtils.deleteDirectory(new File(dir))
		  downloadRepo(dir, uri)
		  
		  // Extract package description
		  val packageFile = new File(dir + "/package.json")
		  if(!packageFile.exists()) 
		    status = "No package.json"		  
		  
		
		  val packageString = handleEncodingWeirdness(packageFile)
	      val packageJson = parse(packageString).extract[packageJSON]
		  
		  // Combine and compress
		  val combined = packageJson.files.foldLeft("") {(combined, n) 
		    => combined + handleEncodingWeirdness(new File(dir + "/" + n)) + " "}
		  
		  val compressed = compress(combined)
		  
		  compressed match {
		    case Full(c) =>
		      // No syntax errors - Upload all versions
		      code.model.Module where (_.repository eqs uri) get() map(
			      existingModule =>
			  		filenamesForVersion(packageJson.name, packageJson.version, existingModule.version.is).foreach(
			  		    filename => upload(filename, c)))
			  upload(filenameForVersion(packageJson.name, "DEBUG"), combined)
		    
		    case Failure(m,e,c) =>
		      // Syntax errors - Upload DEBUG and prepend errors
		      upload(filenameForVersion(packageJson.name, "DEBUG"), "/*\n The following errors prevented us from minify and upload your files:" + m + "\n*/\n" + combined)
		    
		    case Empty =>
		      // This is unlucky
		      upload(filenameForVersion(packageJson.name, "DEBUG"), "/*\n An unhandled error caused your compressed files to not be uploaded... \n*/\n" + combined)
		  }
			  
		  // Save meta data
		  saveMetaData(packageJson, uri)
		  
	  } catch {
	  	case e => if(status == "OK") status = "Error: " + e.getMessage()
	  }
	  println(status)

	}
	
	def downloadRepo(dir: String, uri:String) = {
	  val repo = new FileRepositoryBuilder()
	      .setGitDir(new File("/"))
	      .readEnvironment()
	      .findGitDir()
	      .build()
	      
	    val clone = api.Git.cloneRepository()
	    clone.setBare(false)
	    clone.setCloneAllBranches(false)
	    clone.setDirectory(new File(dir)).setURI(uri)
	    val git = clone.call()
	    git.getRepository().close()
	}
	
	def functionize(js:String): String = {
	  "function() { " + js + " }()"
	}
	
	// Java does not support UTF8 Byte Order Mark. Gues there is a library for that... 
	// http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
	def handleEncodingWeirdness(file:File): String = {
	  IOUtils.toString(new BOMInputStream(new FileInputStream(file)))
	}
	
	def compress(content:String) : Box[String] = {
	  var errors = ""
	  try{
		  
		  val compressor = new JavaScriptCompressor(new StringReader(content),
		      new ErrorReporter() {
			  	def warning(message:String, sourceName:String, line: Int, lineSource: String, lineOffset:Int) {
	                if (line < 0) {
	                    errors += ("\n[WARNING] " + message);
	                } else {
	                    errors += ("\n[WARNING] " + line + ':' + lineOffset + ':' + message + ':' + lineSource);
	                }
	            }
	
	            def error(message:String, sourceName:String, line: Int, lineSource: String, lineOffset:Int) {
	                if (line < 0) {
	                    errors += ("\n[ERROR] " + message);
	                } else {
	                    errors += ("\n[ERROR] " + line + ':' + lineOffset + ':' + message + ':' + lineSource);
	                }
	            }
	
	            def runtimeError (message:String, sourceName:String, line: Int, lineSource: String, lineOffset:Int): EvaluatorException = {
	                error(message, sourceName, line, lineSource, lineOffset);
	                new EvaluatorException(message);
	            }
		  })
	  
		  val str = new ByteArrayOutputStream()
		  val out = new OutputStreamWriter(str)
		  compressor.compress(out, -1, true, false, false, false)
		  out.flush()
		  Full(str.toString())
	  } catch {
	    case e => Failure(errors, Full(e), Empty)
	  }
	  
	}
	
	def upload(key:String, content: String) = {
	  val AWSCredentials = new AWSCredentials(Props.get("AWS.api") openOr System.getenv("AWS.api"), Props.get("AWS.secret") openOr System.getenv("AWS.secret"))
	  val s3Service = new RestS3Service(AWSCredentials)
	  val bucket = s3Service.getBucket("cdn.crafty-modules.com")
	  val moduleObj = new S3Object(bucket, key, content)
	  s3Service.putObject(bucket, moduleObj)
	}
	
	def filenamesForVersion(name:String, version:String, oldVersion:String) = {
	  var filenames = List[String]()
	  filenames ::= filenameForVersion(name, version)
	  if(version.contains("."))
		  filenames ::= filenameForVersion(name, version.substring(0,version.lastIndexOf(".")) + ".x")
	  filenames ::= filenameForVersion(name, "DEV")
	  if(version != oldVersion)
		  filenames ::= filenameForVersion(name, "RELEASE")
		  
	  filenames
	}
	
	def filenameForVersion(name: String, version: String) = {
	  "%s-%s.js".format(name.toLowerCase(), version)
	}
	
	def saveMetaData(desc:packageJSON, repo:String) = {
	  val query = code.model.Module where (_.repository eqs repo)
	  query.findAndDeleteOne()
	  
	  code.model.Module.createRecord
	  	.name(desc.name)
	  	.version(desc.version)
	  	.repository(repo)
	  	.description(desc.description.getOrElse(""))
	  	.jsfiddle(desc.jsfiddle.getOrElse(""))
	  	.author(desc.author match { 
	  	  case Some(p:Person) => code.model.PersonBson.createRecord.name(p.name).email(p.email.getOrElse("")).url(p.url.getOrElse(""))
	  	  case _ => code.model.PersonBson.createRecord.name("").email("").url("")
	  	  })
	  	.files(desc.files)
	  	.homepage(desc.homepage.getOrElse(""))
	  	.keywords(desc.keywords)
	  	.licenses(desc.licenses.map(l => code.model.LicenseBson.createRecord.licenseType(l.`type`).url(l.url)))
	  	.title(desc.title)
	  	.save
	  
	}
}