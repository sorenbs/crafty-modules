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

case class packageJSON(files: List[String])
import net.iharder
object Git {
	implicit val formats = DefaultFormats // Brings in default date formats etc.
	
	def fetch(uri:String) = {
	  var status = "OK"
	  try {
		  val dir = "repos/" + uri.split("/").last
		  FileUtils.deleteDirectory(new File(dir))
		  
		  downloadRepo(dir, uri)
		  
		  val packagejson = new File(dir + "/package.json")
		  if(!packagejson.exists()) 
		    status = "No package.json"
		  
		  val packageDescription = parse(FileUtils.readFileToString(packagejson)).extract[packageJSON]
		  val combined = packageDescription.files.foldLeft("") {(combined, n) 
		    => combined + handleEncodingWeirdness(FileUtils.readFileToString(new File(dir + "/" + n))) + " "}
		  
		  println (compress(combined))
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
	
	def upload = {
//	  val AWSCredentials = new AWSCredentials()
	}
}