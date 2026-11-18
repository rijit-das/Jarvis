package com.jarvis.ai.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentEngineTest {

    private val engine = IntentEngine()

    @Test
    fun `classifies time request`() {
        val intent = engine.classify("What time is it?")
        assertEquals(IntentType.TIME_NOW, intent.type)
    }

    @Test
    fun `classifies reminder with duration and message`() {
        val intent = engine.classify("Remind me to call Sam in 10 minutes")
        assertEquals(IntentType.SET_REMINDER, intent.type)
        assertEquals("10 minutes", intent.entities["duration"])
        assertTrue(intent.entities["message"]!!.contains("call Sam"))
    }

    @Test
    fun `classifies timer`() {
        val intent = engine.classify("Set a timer for 5 minutes")
        assertEquals(IntentType.SET_TIMER, intent.type)
        assertEquals("5 minutes", intent.entities["duration"])
    }

    @Test
    fun `classifies note creation`() {
        val intent = engine.classify("Note that the wifi password is in the drawer")
        assertEquals(IntentType.CREATE_NOTE, intent.type)
        assertTrue(intent.entities["body"]!!.contains("wifi password"))
    }

    @Test
    fun `classifies open app`() {
        val intent = engine.classify("Open camera")
        assertEquals(IntentType.OPEN_APP, intent.type)
        assertEquals("camera", intent.entities["app"])
    }

    @Test
    fun `classifies remember and recall`() {
        assertEquals(IntentType.REMEMBER, engine.classify("Remember that my birthday is June 3").type)
        assertEquals(IntentType.RECALL, engine.classify("What do you remember about birthday?").type)
    }

    @Test
    fun `classifies media commands`() {
        assertEquals(IntentType.PLAY_MEDIA, engine.classify("Play music").type)
        assertEquals(IntentType.PAUSE_MEDIA, engine.classify("Pause the music").type)
        assertEquals(IntentType.NEXT_TRACK, engine.classify("Next song").type)
    }

    @Test
    fun `classifies navigation`() {
        val intent = engine.classify("Navigate to Central Park")
        assertEquals(IntentType.NAVIGATE, intent.type)
        assertEquals("Central Park", intent.entities["destination"])
    }

    @Test
    fun `unknown input returns UNKNOWN with zero confidence`() {
        val intent = engine.classify("asdf qwer zxcv")
        assertEquals(IntentType.UNKNOWN, intent.type)
        assertEquals(0f, intent.confidence)
    }

    @Test
    fun `empty input returns UNKNOWN`() {
        assertEquals(IntentType.UNKNOWN, engine.classify("").type)
        assertEquals(IntentType.UNKNOWN, engine.classify("   ").type)
    }

    @Test
    fun `greetings classify correctly`() {
        assertEquals(IntentType.GREETING, engine.classify("Hello").type)
        assertEquals(IntentType.GREETING, engine.classify("Hey JARVIS").type)
        assertEquals(IntentType.THANK_YOU, engine.classify("Thank you").type)
    }
}
