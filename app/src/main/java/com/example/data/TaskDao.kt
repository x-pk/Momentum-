package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY id ASC")
    fun getStepsForTaskFlow(taskId: Int): Flow<List<TaskStep>>

    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY id ASC")
    suspend fun getStepsForTask(taskId: Int): List<TaskStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<TaskStep>)

    @Query("DELETE FROM task_steps WHERE taskId = :taskId")
    suspend fun deleteStepsForTask(taskId: Int)

    @Update
    suspend fun updateStep(step: TaskStep)
}
