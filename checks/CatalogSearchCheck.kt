package com.cinebox.tv
import kotlinx.coroutines.*
private fun sample(id: Int, title: String, category: String) = Movie(id=id,title=title,category=category,overview="",poster="",backdrop="",year="",score=0.0,genreIds=emptyList())
fun main() = runBlocking {
    val movies = listOf(sample(1,"Malcolm", "Dizi"), sample(2,"Normal Bir Film", "Film"), sample(3,"Malazgirt", "Film"), sample(4,"Elmalı", "Yerli Diziler"), sample(5,"İstanbul", "Film"), sample(6,"Işık", "Film"))
    val index = CatalogSearch.build(movies + movies.first())
    check(index.find("Tümü", "mal").map { it.id }.toSet() == setOf(1,2,3,4))
    check(index.find("Filmler", "mal").map { it.id }.toSet() == setOf(2,3))
    check(index.find("Diziler", "MAL").map { it.id }.toSet() == setOf(1,4))
    check(index.find("Yerli Diziler", "mal").single().id == 4)
    check(index.find("Tümü", "istanbul").single().id == 5)
    check(index.find("Tümü", "ışık").single().id == 6)
    check(index.find("Tümü", "  İSTANBUL  ").single().id == 5)
    check(index.find("Tümü", "xyz").isEmpty())
    check(index.find("Tümü", "").size == 6)
    val sorted = CatalogSearch.build(listOf(sample(1,"Üzüm","Film"),sample(2,"Çilek","Film"),sample(3,"Armut","Film")))
    check(sorted.find("Tümü", "").map { it.title } == listOf("Armut","Çilek","Üzüm"))
    val large = CatalogSearch.build((1..40000).map { sample(it,"Film $it", "Film") })
    check(large.find("Filmler", "40000").single().id == 40000)
    val canceled = launch(start=CoroutineStart.LAZY) { large.find("Tümü", "") ; error("Canceled search ran") }
    canceled.cancel(); canceled.join(); check(canceled.isCancelled)
    println("PASS: Turkish substring/category/sort, duplicate IDs, empty query, 40,000 records, canceled job")
}
