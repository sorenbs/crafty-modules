package code.backend

import net.liftweb.http._
import net.liftweb.http.rest._
import net.liftweb.common.Box
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonParser
import net.liftweb.common.Full


object Webhook extends RestHelper {
	def process: Box[LiftResponse] = {
	    case class Payload(repository: Repository, after: String)
	    case class Repository(url: String, name: String, description: String)
	
	    //According to http://help.github.com/post-receive-hooks/
	    val json = JsonParser.parse(S.param("payload").openOr(""))
	    val repoUrl = json.extract[Payload].repository.url
	    Git.fetch(repoUrl)
	    
	    Full(AcceptedResponse())
	}

}