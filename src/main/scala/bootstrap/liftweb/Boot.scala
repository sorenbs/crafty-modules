package bootstrap.liftweb

import net.liftweb._
import com.foursquare.rogue.Rogue._
import mongodb.{MongoDB, DefaultMongoIdentifier, MongoAddress, MongoHost}
import util._
import Helpers._
import common._
import http._
import sitemap._
import Loc._
import mapper._
import code.model._
import code.snippet.ParamInfo
import com.mongodb.Mongo
import com.mongodb.ServerAddress
import com.mongodb.MongoOptions

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */

class Boot {
  def boot {
    
	  val srvr = new ServerAddress(
	  Props.get("mongodb.url") openOr "localhost",
	  Props.getInt("mongodb.port") openOr 27177)
	
    MongoDB.defineDbAuth(
        DefaultMongoIdentifier, 
        new Mongo(srvr), 
        Props.get("mongodb.db") openOr "crafty-modules",
        Props.get("mongodb.user") openOr "crafty",
        Props.get("mongodb.pass") openOr System.getenv("mongodb.pass"))

      
    // where to search snippet
    LiftRules.addToPackages("code")

    // Build SiteMap
    def sitemap = SiteMap(
      Menu.param[ParamInfo]("module","module", s => Full(ParamInfo(s)), pi => pi.param) / "module",
      Menu.i("Browse") / "index",
      Menu.i("Submit") / "submit",
      Menu.i("Documentation") / "documentation",
      //Menu.i("Home") / "index" >> User.AddUserMenusAfter, // the simple way to declare a menu

      // more complex because this menu allows anything in the
      // /static path to be visible
      Menu(Loc("Static", Link(List("static"), true, "/static/index"), 
	       "Static Content", Hidden))
	  )

    //def sitemapMutators = User.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemap)

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => false)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    // Make a transaction span the whole HTTP request
    //S.addAround(DB.buildLoanWrapper)
  }
}
