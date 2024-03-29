function ThumbsView(thumbSize) {
  function updateLayout() {
    var photoWidth = thumbSize + 10
    var maxWidth = window.innerWidth
    if (isDesktop()) maxWidth -= 90
    var photosInRow = Math.floor(maxWidth / photoWidth)
    $('#content').width(photosInRow * photoWidth)
  }

  function isDesktop() {
    return window.innerWidth > 700
  }

  function isHdpi() {
    return isDesktop() && window.devicePixelRatio >= 2
  }

  function loadThumbs(hdpi) {
    $('.thumbs img').each(function(i, img) {
      if (!img.src) {
        var src = img.dataset.src
        img.src = hdpi ? src.replace('/s' + thumbSize + '-c/', '/s' + (thumbSize * 2) + '-c/') : src
        delete img.dataset.src
      }
    })
  }

  updateLayout()
  loadThumbs(isHdpi())
  $(window).on('resize', updateLayout)
}
