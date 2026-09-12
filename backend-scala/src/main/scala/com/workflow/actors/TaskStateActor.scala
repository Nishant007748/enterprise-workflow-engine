package com.workflow.actors

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import com.workflow.domain.{Task, TaskState}
import com.workflow.db.Tables
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object TaskStateActor {
  sealed trait Command
  final case class CreateTask(task: Task, replyTo: ActorRef[Response]) extends Command
  final case class GetTasks(replyTo: ActorRef[Response]) extends Command
  final case class UpdateTaskState(id: Long, newState: String, replyTo: ActorRef[Response]) extends Command
  
  private final case class InternalTaskCreated(task: Task, replyTo: ActorRef[Response]) extends Command
  private final case class InternalTasksRetrieved(tasks: Seq[Task], replyTo: ActorRef[Response]) extends Command
  private final case class InternalTaskUpdated(task: Option[Task], replyTo: ActorRef[Response]) extends Command
  private final case class InternalError(reason: Throwable, replyTo: ActorRef[Response]) extends Command

  sealed trait Response
  final case class TaskCreated(task: Task) extends Response
  final case class TasksList(tasks: Seq[Task]) extends Response
  final case class TaskUpdated(task: Option[Task]) extends Response
  final case class ActionFailed(reason: String) extends Response

  def apply(db: Database)(implicit ec: ExecutionContext): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case CreateTask(task, replyTo) =>
        val taskWithDefaultState = task.copy(state = TaskState.TODO.toString)
        val insertQuery = (Tables.tasks returning Tables.tasks.map(_.id) into ((t, id) => t.copy(id = id))) += taskWithDefaultState
        
        context.pipeToSelf(db.run(insertQuery)) {
          case Success(insertedTask) => InternalTaskCreated(insertedTask, replyTo)
          case Failure(ex) => InternalError(ex, replyTo)
        }
        Behaviors.same

      case GetTasks(replyTo) =>
        context.pipeToSelf(db.run(Tables.tasks.result)) {
          case Success(tasks) => InternalTasksRetrieved(tasks, replyTo)
          case Failure(ex) => InternalError(ex, replyTo)
        }
        Behaviors.same

      case UpdateTaskState(id, newState, replyTo) =>
        // Strict state transition validation could be added here
        val updateQuery = Tables.tasks.filter(_.id === id).map(_.state).update(newState)
        
        val futureResult: Future[Option[Task]] = db.run(updateQuery).flatMap {
          case 0 => Future.successful(None)
          case _ => db.run(Tables.tasks.filter(_.id === id).result.headOption)
        }

        context.pipeToSelf(futureResult) {
          case Success(task) => InternalTaskUpdated(task, replyTo)
          case Failure(ex) => InternalError(ex, replyTo)
        }
        Behaviors.same

      case InternalTaskCreated(task, replyTo) =>
        replyTo ! TaskCreated(task)
        Behaviors.same

      case InternalTasksRetrieved(tasks, replyTo) =>
        replyTo ! TasksList(tasks)
        Behaviors.same

      case InternalTaskUpdated(task, replyTo) =>
        replyTo ! TaskUpdated(task)
        Behaviors.same

      case InternalError(ex, replyTo) =>
        replyTo ! ActionFailed(ex.getMessage)
        Behaviors.same
    }
  }
}
