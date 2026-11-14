package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName

/** `GET social/blocks`: autores que el usuario bloqueó, con su nombre público (nunca el completo). */
data class BlockedAuthorsResponse(val data: List<BlockedAuthor> = emptyList())

/** Un bloqueo propio; [id] es el del bloqueo (no el del usuario) y sirve para `DELETE social/blocks/{id}`. */
data class BlockedAuthor(val id: String, val name: String? = null)

/** `GET admin/comment-reports`: un elemento por comentario con reportes pendientes. */
data class CommentReportsResponse(val data: List<ReportedComment> = emptyList())

data class ReportedComment(
    @SerializedName("comment_id") val commentId: String,
    val comment: String? = null,
    val author: ReportedCommentAuthor? = null,
    val work: ReportedCommentWork? = null,
    @SerializedName("reports_count") val reportsCount: Int = 0,
    /** Motivo (`spam`, `ofensivo`, `sexual_violento`, `otro`) y cuántos reportes lo citan. */
    val reasons: Map<String, Int> = emptyMap(),
    @SerializedName("first_reported_at") val firstReportedAt: String? = null
)

data class ReportedCommentAuthor(
    val id: String? = null,
    val name: String? = null,
    @SerializedName("comments_banned") val commentsBanned: Boolean = false
)

data class ReportedCommentWork(val id: String? = null, val title: String? = null)

/** `POST admin/comment-reports/{comment}/resolve`; [action] es `remove`, `remove_and_ban` o `dismiss`. */
data class ResolveReportRequest(val action: String)

data class ResolveReportResponse(
    val message: String? = null,
    @SerializedName("resolved_reports") val resolvedReports: Int = 0
)
