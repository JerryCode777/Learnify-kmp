package org.example.learnify.data.mapper

import com.benasher44.uuid.uuid4
import org.example.learnify.data.remote.model.LearningPathResponse
import org.example.learnify.data.remote.model.TopicResponse
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.model.Topic
import org.example.learnify.util.currentTimeMillis

fun LearningPathResponse.toDomain(documentId: String): LearningPath {
    return LearningPath(
        id = uuid4().toString(),
        documentId = documentId,
        title = this.title,
        topics = this.topics.mapIndexed { index, topicResponse ->
            topicResponse.toDomain(index)
        },
        createdAt = currentTimeMillis()
    )
}

fun TopicResponse.toDomain(order: Int): Topic {
    return Topic(
        id = uuid4().toString(),
        title = this.title,
        content = "${this.description}\n\n${this.content}",
        pageNumbers = emptyList(), // Se puede calcular después si es necesario
        order = order
    )
}
