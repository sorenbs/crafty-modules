package code
package model

import net.liftweb.mongodb.record.{MongoMetaRecord, MongoId, MongoRecord}
import net.liftweb.record.field.{StringField, EnumNameField, LongField}


class Module extends MongoRecord[Module] with MongoId[Module] {
  def meta = Module
  object name extends StringField(this,255)
  object version extends StringField(this,255)
}
object Module extends Module with MongoMetaRecord[Module] {
  def fromGithubJson(json:String) = {

  }
}