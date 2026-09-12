package com.workflow.api

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.util.Timeout
import spray.json.DefaultJsonProtocol._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future
import scala.concurrent.duration._

import com.workflow.actors.TaskStateActor
import com.workflow.domain.Task
import com.workflow.domain.TaskJsonProtocol._

class TaskRoutes(taskStateActor: ActorRef[TaskStateActor.Command])(implicit val system: ActorSystem[_]) {
  
  import system.executionContext
  private implicit val timeout: Timeout = Timeout(5.seconds)

  case class StateUpdateRequest(state: String)
  implicit val stateUpdateFormat = jsonFormat1(StateUpdateRequest)

  val routes: Route =
    pathPrefix("api" / "tasks") {
      concat(
        pathEndOrSingleSlash {
          concat(
            get {
              val tasksFuture: Future[TaskStateActor.Response] = taskStateActor.ask(TaskStateActor.GetTasks)
              onSuccess(tasksFuture) {
                case TaskStateActor.TasksList(tasks) => complete(tasks)
                case TaskStateActor.ActionFailed(reason) => complete(StatusCodes.InternalServerError -> reason)
                case _ => complete(StatusCodes.InternalServerError)
              }
            },
            post {
              entity(as[Task]) { task =>
                val taskCreatedFuture: Future[TaskStateActor.Response] = taskStateActor.ask(ref => TaskStateActor.CreateTask(task, ref))
                onSuccess(taskCreatedFuture) {
                  case TaskStateActor.TaskCreated(createdTask) => complete(StatusCodes.Created -> createdTask)
                  case TaskStateActor.ActionFailed(reason) => complete(StatusCodes.InternalServerError -> reason)
                  case _ => complete(StatusCodes.InternalServerError)
                }
              }
            }
          )
        },
        path(LongNumber / "state") { id =>
          put {
            entity(as[StateUpdateRequest]) { updateReq =>
              val updatedFuture: Future[TaskStateActor.Response] = taskStateActor.ask(ref => TaskStateActor.UpdateTaskState(id, updateReq.state, ref))
              onSuccess(updatedFuture) {
                case TaskStateActor.TaskUpdated(Some(task)) => complete(task)
                case TaskStateActor.TaskUpdated(None) => complete(StatusCodes.NotFound)
                case TaskStateActor.ActionFailed(reason) => complete(StatusCodes.InternalServerError -> reason)
                case _ => complete(StatusCodes.InternalServerError)
              }
            }
          }
        }
      )
    }
}
