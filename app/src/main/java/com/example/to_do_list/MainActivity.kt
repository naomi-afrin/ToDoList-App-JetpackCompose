package com.example.to_do_list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_do_list.ui.theme.To_Do_ListTheme
import kotlinx.coroutines.launch

// Enum to represent theme options (only LIGHT and DARK)
enum class ThemeMode {
    LIGHT, DARK
}

data class Task(val id: Int, val name: String, var isCompleted: Boolean = false, var isStarred: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    // State to track user's theme preference (default is LIGHT)
    var themeMode by remember { mutableStateOf(ThemeMode.LIGHT) }

    // Apply the theme
    To_Do_ListTheme(darkTheme = themeMode == ThemeMode.DARK) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ToDoListScreen(themeMode, onThemeChange = { newThemeMode ->
                    themeMode = newThemeMode
                })

                // Dark Mode Button in the bottom-right corner
                DarkModeButton(
                    themeMode = themeMode,
                    onThemeChange = { newThemeMode ->
                        themeMode = newThemeMode
                    },
                    modifier = Modifier
                        .padding(30.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
fun DarkModeButton(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            // Toggle between LIGHT and DARK modes
            val newThemeMode = when (themeMode) {
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.LIGHT
            }
            onThemeChange(newThemeMode)
        },
        modifier = modifier.size(60.dp) // Button size
    ) {
        // Show moon icon in light mode and sun icon in dark mode
        if (themeMode == ThemeMode.LIGHT) {
            Image(
                painter = painterResource(id = R.drawable.ic_moon),
                contentDescription = "Switch to Dark Mode",
                modifier = Modifier.size(60.dp) // Icon size
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_sun),
                contentDescription = "Switch to Light Mode",
                modifier = Modifier.size(60.dp) // Icon size
            )
        }
    }
}

@Composable
fun ToDoListScreen(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    var taskName by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var taskIdCounter by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Input Row
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Enter task") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    if (taskName.isNotBlank()) {
                        tasks = tasks + Task(taskIdCounter++, taskName)
                        taskName = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task List
        LazyColumn {
            items(tasks.sortedWith(compareBy({ !it.isStarred }, { it.isCompleted })), key = { it.id }) { task ->
                AnimatedVisibility(
                    visible = tasks.contains(task),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TaskItem(task, onTaskDelete = { deletedTask ->
                        tasks = tasks.filter { it.id != deletedTask.id }

                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Task deleted",
                                actionLabel = "Undo"
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                tasks = tasks + deletedTask
                            }
                        }
                    }, onTaskToggle = { toggledTask ->
                        tasks = tasks.map { if (it.id == toggledTask.id) it.copy(isCompleted = !it.isCompleted) else it }
                        // Sort tasks so that completed tasks are at the bottom
                        tasks = tasks.sortedWith(compareBy({ !it.isStarred }, { it.isCompleted }))
                    }, onTaskStar = { starredTask ->
                        tasks = tasks.map { if (it.id == starredTask.id) it.copy(isStarred = !it.isStarred) else it }
                        // Sort tasks so that starred tasks are at the top
                        tasks = tasks.sortedWith(compareBy({ !it.isStarred }, { it.isCompleted }))
                    })
                }
            }
        }

        // Snackbar
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun TaskItem(task: Task, onTaskDelete: (Task) -> Unit, onTaskToggle: (Task) -> Unit, onTaskStar: (Task) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (task.isStarred) Color.hsl(50f, 1f, 0.5f) else MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.name,
                fontSize = 18.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
            Row {
                IconButton(onClick = { onTaskStar(task) }) {
                    Icon(Icons.Default.Star, contentDescription = "Star Task", tint = if (task.isStarred) Color.Yellow else Color.Gray)
                }
                IconButton(onClick = { onTaskToggle(task) }) {
                    Icon(Icons.Default.Check, contentDescription = "Mark as Done")
                }
                IconButton(onClick = { onTaskDelete(task) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Task")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewToDoList() {
    To_Do_ListTheme {
        ToDoListScreen(ThemeMode.LIGHT, onThemeChange = {})
    }
}