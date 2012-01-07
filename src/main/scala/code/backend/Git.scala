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
    dependencies:Map[String,String],
    homepage:Option[String])

object Git {
	implicit val formats = DefaultFormats // Brings in default date formats etc.
	
	def fetch(uri:String) = {
	  var status = "OK"
	  //try {
		  // Delete old and download new
		  val dir = "repos/" + uri.split("/").last
		  FileUtils.deleteDirectory(new File(dir))
		  downloadRepo(dir, uri)
		  
		  // Extract package description
		  val packagejson = new File(dir + "/package.json")
		  if(!packagejson.exists()) 
		    status = "No package.json"		  
	      val packageDescription = parse(FileUtils.readFileToString(packagejson)).extract[packageJSON]
		  
		  // Combine and compress
		  val combined = packageDescription.files.foldLeft("") {(combined, n) 
		    => combined + handleEncodingWeirdness(FileUtils.readFileToString(new File(dir + "/" + n))) + " "}
		  val compressed = compress(combined)
		  
		  // Upload compressed versions
		  val oldVersion = code.model.Module where (_.repository eqs uri) get() map(
		      existingModule =>
		  		filenamesForVersion(packageDescription.name, packageDescription.version, existingModule.version.is).foreach(
		  		    filename => upload(filename, compressed)))
		  
		  // Upload uncompressed for debugging
		  upload(filenameForVersion(packageDescription.name, "DEBUG"), combined)
		  	
		  // Save meta data
		  saveMetaData(packageDescription, uri)
		  
	 // } catch {
	  	//case e => if(status == "OK") status = "Error: " + e.getMessage()
	  //}
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
	
	//Files downloaded from github with jGit are prepended with some weired characters.
	// When read as UTF8 as per below they are printed as a single ?
	//I guess we could handle this in a proper way
	def handleEncodingWeirdness(content:String): String = {
	  val utf = new String(content.getBytes(), "UTF8")
	  if(utf.length() > 0)
	    utf.slice(1, utf.length())
	  else
	    ""
	}
	
	def compress(content:String) = {
	  val compressor = new JavaScriptCompressor(new StringReader(content),
	      new ErrorReporter() {
		  	def warning(message:String, sourceName:String, line: Int, lineSource: String, lineOffset:Int) {
                if (line < 0) {
                    System.err.println("\n[WARNING] " + message);
                } else {
                    System.err.println("\n[WARNING] " + line + ':' + lineOffset + ':' + message);
                }
            }

            def error(message:String, sourceName:String, line: Int, lineSource: String, lineOffset:Int) {
                if (line < 0) {
                    System.err.println("\n[ERROR] " + message);
                } else {
                    System.err.println("\n[ERROR] " + line + ':' + lineOffset + ':' + message);
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
	  str.toString()
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