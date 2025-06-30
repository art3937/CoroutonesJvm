package dto

import ru.netology.coroutines.dto.Author

data class PostWithComments(
    val post: Post,
    val comments: List<Comment>,
    val author: Author
)
