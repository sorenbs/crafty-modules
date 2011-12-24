package code.backend

import org.eclipse.jgit._
import storage.file.FileRepositoryBuilder
import java.io.File
import org.apache.commons.io.FileUtils
import net.liftweb.json._
//import net.liftweb.json.JsonParser._

case class packageJSON(files: List[String])

object Git {
	implicit val formats = DefaultFormats // Brings in default date formats etc.
	
	def fetch(uri:String) = {
	  
	  var status = "OK"
	  val dir = "repos/" + uri.split("/").last
	  FileUtils.deleteDirectory(new File(dir))
	  
	  downloadRepo(dir, uri)
	  
	  val packagejson = new File(dir + "/package.json")
	  if(packagejson.exists()) {
		  val packageDescription = parse(FileUtils.readFileToString(packagejson)).extract[packageJSON]
		  packageDescription.files.foreach(s => println(s))
	  } else {
		  status = "No package.json!"
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
}