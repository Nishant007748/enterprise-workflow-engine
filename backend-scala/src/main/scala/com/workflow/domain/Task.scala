package com.workflow.domain

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object TaskState extends Enumeration {
  type TaskState = Value
  val TODO, IN_PROGRESS, DONE = Value
}

case class Task(id: Option[Long] = None, title: String, description: Option[String] = None, state: String = TaskState.TODO.toString)

object TaskJsonProtocol {
  implicit val taskFormat: RootJsonFormat[Task] = jsonFormat4(Task.apply)
}
