package io.github.porum.asas

import kotlinx.serialization.Serializable

@Serializable
data class Node(
  val owner: String,
  val name: String,
  val descriptor: String,
)