package code.snippet

import net.liftweb.common._
import net.liftweb.util.Helpers._
import java.util.Date
import code.lib._
import code.model._
import net.liftweb.util.ClearClearable


class Module {
	def list = {
	  code.backend.Git.fetch("git@github.com:sorenbs/MoveTo.git")

    ".module_row *" #>
      Module.findAll.map(m =>
        "h2 *" #> m.name &
        ".version *" #> m.version &
        ClearClearable)
  }
}

