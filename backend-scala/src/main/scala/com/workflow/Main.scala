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
import scala.util.{Failure, Success, Try}

object Main {
  private sealed trait Command
  private final case class DatabaseSetupCompleted(result: Try[Unit]) extends Command
  private final case class ServerBindingCompleted(result: Try[Http.ServerBinding]) extends Command

  def main(args: Array[String]): Unit = {
    ActorSystem[Command](Behaviors.setup[Command] { context =>
      implicit val system: ActorSystem[_] = context.system
      implicit val executionContext: ExecutionContextExecutor = system.executionContext

      val db = Database.forConfig("db")
      val guardian = context.self
      db.run(Tables.tasks.schema.createIfNotExists).onComplete { result =>
        guardian ! DatabaseSetupCompleted(result)
      }

      Behaviors.receiveMessage {
        case DatabaseSetupCompleted(Success(_)) =>
          context.log.info("Database schema setup complete")
          val taskStateActor = context.spawn(TaskStateActor(db), "TaskStateActor")
          val taskRoutes = new TaskRoutes(taskStateActor)
          Http().newServerAt("0.0.0.0", 8080).bind(taskRoutes.routes).onComplete { result =>
            guardian ! ServerBindingCompleted(result)
          }
          Behaviors.same
        case DatabaseSetupCompleted(Failure(ex)) =>
          context.log.error("Failed to setup database schema, terminating system", ex)
          system.terminate()
          Behaviors.same
        case ServerBindingCompleted(Success(binding)) =>
          val address = binding.localAddress
          context.log.info(s"Server online at http://${address.getHostString}:${address.getPort}/")
          Behaviors.same
        case ServerBindingCompleted(Failure(ex)) =>
          context.log.error("Failed to bind HTTP endpoint, terminating system", ex)
          system.terminate()
          Behaviors.same
      }
    }, "workflow-system")
  }
}
