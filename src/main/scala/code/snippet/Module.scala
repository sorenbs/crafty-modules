package code.snippet

import net.liftweb.common._
import net.liftweb.util.Helpers._
import java.util.Date
import code.lib._
import code.model._
import net.liftweb.util.ClearClearable
import scala.xml.NodeSeq
import com.foursquare.rogue.Rogue._
import net.liftweb.mongodb.record._
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import net.liftweb.record._

case class ParamInfo(param: String)

class Modules {
	def list = {
	  code.backend.Git.fetch("git@github.com:sorenbs/MoveTo.git")

	  ".module_row *" #>
      	Module.findAll.map(m =>
  			".name *" #> m.name &
  			".name [href]" #> ("/module/" + m.name) &
  			".description *" #> m.description &
  			".keywords *" #> m.keywords.is.foldLeft("") {(combined, n) => combined + (if(combined.length > 0) ", " else "") + n} &
  			ClearClearable)
  }
	
	def infoList(pi: ParamInfo) = {
	  "info_row *" #> pi.param
	}
}

class Module(pi: ParamInfo) {
	val module = Module.where(_.name eqs pi.param).fetch(1)
	
	def info = {
	  "*" #>
	  	module.map(m =>
	  	  ".name *" #> m.name &
	  	  ".description *" #> m.description &
	  	  ClearClearable)
	}
	
  def infoList = {
	  "*" #>
	  	module.map(m =>
	  	  ".title *" #> m.title &
	  	  ".version *" #> m.version &
	  	  ".author *" #> m.author.is.name &
	  	  ".licenses *" #> m.licenses.is.map(l =>
	  	    "a *" #> l.licenseType &
	  	    "a [href]" #> l.url) &
	  	  ".keywords *" #> m.keywords.is.foldLeft("") {(combined, n) => combined + (if(combined.length > 0) ", " else "") + n} &
	  	  ".homepage *" #> List(m.homepage.is).map(h =>
	  	    "a *" #> h &
	  	    "a [href]" #> h) &
	  	  ".download *" #> List("http://cdn.crafty-modules.com/%s-RELEASE.js".format(m.name.is.toLowerCase())).map(u =>
	  	    "a *" #> "download" & 
	  	    "a [href]" #> u) &
	  	  ClearClearable)
	}
	
	def fiddle = {
	  "*" #> 
	  	module.map(m =>
	    "iframe [src]" #> m.jsfiddle)
	}
}