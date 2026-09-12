package com.workflow

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives._
import slick.jdbc.PostgresProfile.api._
import com.workflow.actors.TaskStateActor
import com.workflow.api.TaskRoutes
import com.workflow.db.Tables

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[_] = context.system
      implicit val executionContext: ExecutionContextExecutor = system.executionContext

      // Database Setup
      val db = Database.forConfig("db")
      
      // Auto schema generation for simplicity
      val setupFuture = db.run(Tables.tasks.schema.createIfNotExists)
      setupFuture.onComplete {
        case Success(_) => context.log.info("Database schema setup complete")
        case Failure(ex) => context.log.error(s"Failed to setup database schema: ${ex.getMessage}")
      }

      // Initialize Actor
      val taskStateActor = context.spawn(TaskStateActor(db), "TaskStateActor")

      // Initialize Routes
      val taskRoutes = new TaskRoutes(taskStateActor)
      
      val routes = taskRoutes.routes

      // Start Server
      val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(routes)
      
      bindingFuture.onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          context.log.info(s"Server online at http://${address.getHostString}:${address.getPort}/")
        case Failure(ex) =>
          context.log.error(s"Failed to bind HTTP endpoint, terminating system", ex)
          system.terminate()
      }

      Behaviors.empty
    }, "workflow-system")
  }
}
