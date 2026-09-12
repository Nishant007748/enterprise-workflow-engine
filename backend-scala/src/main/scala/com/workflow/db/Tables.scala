package com.workflow.db

import com.workflow.domain.Task
import slick.jdbc.PostgresProfile.api._

class TasksTable(tag: Tag) extends Table[Task](tag, "tasks") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def description = column[Option[String]]("description")
  def state = column[String]("state")

  def * = (id, title, description, state) <> (Task.tupled, Task.unapply)
}

object Tables {
  val tasks = TableQuery[TasksTable]
}
