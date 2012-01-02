import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler, FilterMapping}
import org.eclipse.jetty.server.nio.SelectChannelConnector
import net.liftweb.http.LiftFilter
import java.util.EnumSet
import javax.servlet.DispatcherType

object JettyLauncher extends Application {
  val DefaultDispatcherTypes: EnumSet[DispatcherType] = 
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)

  val port = if(System.getenv("PORT") != null) System.getenv("PORT").toInt else 8080
  val server = new Server
  val scc = new SelectChannelConnector
  scc.setPort(port)
  server.setConnectors(Array(scc))

  val context = new WebAppContext// ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS)
  context.setServer(server)
  context.addServlet(classOf[DefaultServlet], "/");
  context.addFilter(classOf[LiftFilter], "/*", DefaultDispatcherTypes)
  context.setResourceBase("src/main/webapp")
  
  context.setContextPath("/")
  context.setWar("src/main/webapp")

  server.setHandler(context)
  
  server.start
  server.join
}
