package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.beans.propertyeditors.CustomCollectionEditor

class SplitCollectionEditor(
  private val collectionType: Class<out MutableCollection<*>?>,
  private val splitRegex: String,
) : CustomCollectionEditor(collectionType, true) {

  override fun setAsText(text: String) {
    if (text.isEmpty()) {
      super.setValue(super.createCollection(this.collectionType, 0))
    } else {
      super.setValue(text.split(splitRegex.toRegex()).toTypedArray())
    }
  }
}
