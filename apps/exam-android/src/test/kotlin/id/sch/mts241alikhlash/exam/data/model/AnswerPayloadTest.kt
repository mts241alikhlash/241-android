package id.sch.mts241alikhlash.exam.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnswerPayloadTest {
    @Test
    fun choice_round_trips() {
        val payload = AnswerPayload.Choice("B")
        assertEquals("""{"key":"B"}""", payload.encode())
        assertEquals(payload, AnswerPayload.decode(payload.encode()))
    }

    @Test
    fun multiple_is_sorted_and_round_trips() {
        val payload = AnswerPayload.Multiple(listOf("C", "A"))
        assertEquals("""{"keys":["A","C"]}""", payload.encode())
        assertEquals(AnswerPayload.Multiple(listOf("A", "C")), AnswerPayload.decode(payload.encode()))
    }

    @Test
    fun bool_round_trips() {
        assertEquals("""{"value":true}""", AnswerPayload.Bool(true).encode())
        assertEquals(AnswerPayload.Bool(false), AnswerPayload.decode("""{"value":false}"""))
    }

    @Test
    fun text_round_trips_with_quotes_and_newlines() {
        val payload = AnswerPayload.Text("a \"b\"\nc")
        assertEquals(payload, AnswerPayload.decode(payload.encode()))
    }

    @Test
    fun foreign_json_decodes_to_null() {
        assertNull(AnswerPayload.decode("""{"foo":1}"""))
        assertNull(AnswerPayload.decode("""{"key":1}"""))
        assertNull(AnswerPayload.decode("""{"key":"A","value":true}"""))
        assertNull(AnswerPayload.decode("not json"))
        assertNull(AnswerPayload.decode("Paris"))
    }

    @Test
    fun json_object_form_matches_the_wire_shape() {
        assertEquals(AnswerPayload.Choice("D"), AnswerPayload.fromJson(AnswerPayload.Choice("D").toJson()))
    }
}
