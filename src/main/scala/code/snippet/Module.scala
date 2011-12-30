package code.snippet

import net.liftweb.common._
import net.liftweb.util.Helpers._
import java.util.Date
import code.lib._
import code.model._
import net.liftweb.util.ClearClearable
import scala.xml.NodeSeq


class Module {
	def list = {
	  code.backend.Git.fetch("git@github.com:sorenbs/MoveTo.git")

	  ".module_row *" #>
      	Module.findAll.map(m =>
  			".name *" #> m.name &
  			".description *" #> m.description &
  			".keywords *" #> m.keywords.is.foldLeft("") {(combined, n) => combined + (if(combined.length > 0) ", " else "") + n} &
  			ClearClearable)
  }
	
	def m() = {
	  "*" #> net.liftweb.builtin.snippet.Menu.builder(<div class="lift:module.m?li:class=menu_item;li_item:class=active;ul:class=nav;linkToSelf=true" />)
	}
}

