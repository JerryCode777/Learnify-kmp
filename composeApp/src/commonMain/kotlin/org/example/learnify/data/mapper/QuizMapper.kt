package org.example.learnify.data.mapper

import com.benasher44.uuid.uuid4
import org.example.learnify.data.remote.model.QuestionResponse
import org.example.learnify.data.remote.model.QuizResponse
import org.example.learnify.domain.model.Question
import org.example.learnify.domain.model.Quiz

fun QuizResponse.toDomain(topicId: String): Quiz {
    return Quiz(
        id = uuid4().toString(),
        topicId = topicId,
        questions = this.questions.map { it.toDomain() }
    )
}

fun QuestionResponse.toDomain(): Question {
    return Question(
        id = uuid4().toString(),
        text = this.question,
        options = this.options,
        correctAnswer = this.correctAnswer,
        explanation = this.explanation
    )
}
