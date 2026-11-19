package com.jarvis.core.di

import com.jarvis.tools.ToolRegistry
import com.jarvis.tools.impl.AddTaskTool
import com.jarvis.tools.impl.CompleteTaskTool
import com.jarvis.tools.impl.CreateNoteTool
import com.jarvis.tools.impl.DateTool
import com.jarvis.tools.impl.ForgetTool
import com.jarvis.tools.impl.ListNotesTool
import com.jarvis.tools.impl.ListTasksTool
import com.jarvis.tools.impl.MediaTool
import com.jarvis.tools.impl.NavigateTool
import com.jarvis.tools.impl.NotificationTool
import com.jarvis.tools.impl.OpenAppTool
import com.jarvis.tools.impl.RecallTool
import com.jarvis.tools.impl.RememberTool
import com.jarvis.tools.impl.ReminderTool
import com.jarvis.tools.impl.SearchNotesTool
import com.jarvis.tools.impl.TimeTool
import com.jarvis.tools.impl.TimerTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Builds the tool allowlist. Only tools registered here can ever execute.
 */
@Module
@InstallIn(SingletonComponent::class)
object ToolModule {

    @Provides
    @Singleton
    fun provideToolRegistry(
        timeTool: TimeTool,
        dateTool: DateTool,
        timerTool: TimerTool,
        reminderTool: ReminderTool,
        createNoteTool: CreateNoteTool,
        listNotesTool: ListNotesTool,
        searchNotesTool: SearchNotesTool,
        addTaskTool: AddTaskTool,
        listTasksTool: ListTasksTool,
        completeTaskTool: CompleteTaskTool,
        openAppTool: OpenAppTool,
        mediaPlay: MediaTool.PlayTool,
        mediaPause: MediaTool.PauseTool,
        mediaNext: MediaTool.NextTool,
        mediaPrevious: MediaTool.PreviousTool,
        navigateTool: NavigateTool,
        notificationTool: NotificationTool,
        rememberTool: RememberTool,
        recallTool: RecallTool,
        forgetTool: ForgetTool
    ): ToolRegistry {
        return ToolRegistry().apply {
            register(timeTool)
            register(dateTool)
            register(timerTool)
            register(reminderTool)
            register(createNoteTool)
            register(listNotesTool)
            register(searchNotesTool)
            register(addTaskTool)
            register(listTasksTool)
            register(completeTaskTool)
            register(openAppTool)
            register(mediaPlay)
            register(mediaPause)
            register(mediaNext)
            register(mediaPrevious)
            register(navigateTool)
            register(notificationTool)
            register(rememberTool)
            register(recallTool)
            register(forgetTool)
        }
    }
}
