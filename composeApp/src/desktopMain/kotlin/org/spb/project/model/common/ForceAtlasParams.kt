package org.spb.project.model.common

data class ForceAtlasParams(
    val repulsion: Double,
    val attraction: Double,
    val damping: Double,
    val gravity: Double,
    val maxDisplacement: Double
)