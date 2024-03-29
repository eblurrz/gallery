package net.azib.photos

import java.lang.Math.min
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.*

class Picasa(user: String? = null, private val authKey: String? = null) {
  val user: String = user ?: defaultUser

  val urlPrefix: String
    get() = "/${user}"

  val urlSuffix: String
    get() = if (user != defaultUser) "?by=${user}" else ""

  val analytics: String?
    get() = config.getProperty("google.analytics")

  val mapsKey: String?
    get() = config.getProperty("google.maps.key")

  val gallery: Gallery
    get() {
      val thumbSize = 212
      val url = "?kind=album&thumbsize=${thumbSize}c" +
                "&fields=id,updated,gphoto:*,entry(title,summary,updated,content,category,gphoto:*,media:*,georss:*)"
      return GalleryLoader(thumbSize).load(url)
    }

  fun getAlbum(name: String): Album {
    val thumbSize = 144
    val id = gallery.albums[name]?.id ?: name
    val path = if (id.matches("\\d+".toRegex())) "/albumid/" + id else "/album/" + urlEncode(name)
    val url = path + "?kind=photo,comment&imgmax=1600&thumbsize=${thumbSize}c&max-results=500" +
      "&fields=id,updated,title,subtitle,icon,gphoto:*,georss:where(gml:Point),entry(title,summary,content,author,category,gphoto:id,gphoto:photoid,gphoto:width,gphoto:height,gphoto:commentCount,gphoto:timestamp,exif:*,media:*,georss:where(gml:Point))"
    val loader = AlbumLoader(thumbSize)
    val album = loader.load(url)
    while (album.size > album.photos.size)
      loader.load(url + "&start-index=${album.photos.size+1}")
    return album
  }

  fun getRandomPhotos(numNext: Int): RandomPhotos {
    val album = weightedRandom(gallery.albums.values)
    val photos = getAlbum(album.name!!).photos
    val index = random(photos.size)
    return RandomPhotos(photos.subList(index, min(index + numNext, photos.size)), album.author, album.title)
  }

  fun weightedRandom(albums: Collection<Album>): Album {
    var sum = 0
    for (album in albums) sum += transform(album.size().toDouble())
    val index = random(sum)

    sum = 0
    for (album in albums) {
      sum += transform(album.size().toDouble())
      if (sum > index) return album
    }
    return albums.first()
  }

  private fun transform(n: Double): Int {
    return (100.0 * Math.log10(1 + n / 50.0)).toInt()
  }

  internal fun random(max: Int): Int {
    return if (max == 0) 0 else random.nextInt(max)
  }

  fun search(query: String): Album {
    val thumbSize = 144
    return AlbumLoader(thumbSize).load("?kind=photo&q=" + urlEncode(query) + "&imgmax=1024&thumbsize=${thumbSize}c")
  }

  private fun <T: Entity> XMLListener<T>.load(query: String)
      = URLLoader.load(toFullUrl(query), this)

  private fun toFullUrl(query: String): String {
    var url = "http://picasaweb.google.com/data/feed/api/user/" + urlEncode(user) + query
    if (authKey != null) url += (if (url.contains("?")) "&" else "?") + "authkey=" + authKey
    return url
  }

  private fun urlEncode(name: String): String {
    return URLEncoder.encode(name, "UTF-8")
  }

  companion object {
    internal var config = loadConfig()
    internal var defaultUser = config.getProperty("google.user")
    internal var random: Random = SecureRandom()

    private fun loadConfig(): Properties {
      val config = Properties()
      config.load(Picasa::class.java.getResourceAsStream("/config.properties"))
      return config
    }
  }
}
