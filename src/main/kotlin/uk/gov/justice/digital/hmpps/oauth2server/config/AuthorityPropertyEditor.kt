package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle
import java.beans.PropertyChangeListener
import java.beans.PropertyEditor

class AuthorityPropertyEditor : PropertyEditor {
  private var grantedAuthority: GrantedAuthority? = null

  override fun setValue(value: Any) {
    grantedAuthority = value as GrantedAuthority
  }

  override fun getValue(): Any? = grantedAuthority

  override fun isPaintable(): Boolean = false

  override fun paintValue(gfx: Graphics?, box: Rectangle?) {}
  override fun getJavaInitializationString(): String? = null

  override fun getAsText(): String? = grantedAuthority?.authority

  override fun setAsText(text: String?) {
    if (!text.isNullOrEmpty()) {
      grantedAuthority = SimpleGrantedAuthority(text)
    }
  }

  override fun getTags(): Array<String?> = arrayOfNulls(0)

  override fun getCustomEditor(): Component? = null

  override fun supportsCustomEditor(): Boolean = false

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
  override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
}
