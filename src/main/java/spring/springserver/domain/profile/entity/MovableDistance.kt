package spring.springserver.domain.profile.entity

enum class MovableDistance(private val label: String) {

    TEN_KM("10km"),
    TWENTY_FIVE_KM("25km"),
    FIFTY_KM("50km"),
    OVER_HUNDRED_KM("100km 이상");

    fun getLabel(): String = label
}