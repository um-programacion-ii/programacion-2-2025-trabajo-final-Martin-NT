package ar.edu.um.tpfinal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform