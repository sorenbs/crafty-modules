package code
package model

import net.liftweb.mongodb.record.{MongoMetaRecord, MongoId, MongoRecord}
import net.liftweb.record.field.{StringField, EnumNameField, LongField}
import net.liftweb.mongodb.record.field.{MongoListField, BsonRecordField, BsonRecordListField,MongoMapField}
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.mongodb.record.BsonMetaRecord


class Module extends MongoRecord[Module] with MongoId[Module] {
  def meta = Module
  object name extends StringField(this,255)
  object version extends StringField(this,255)
  object description extends StringField(this,4100)
  object jsfiddle extends StringField(this,512)
  object files extends MongoListField[Module, String](this)
  object title extends StringField(this, 255)
  object author extends BsonRecordField(this,PersonBson)
  object contributors extends BsonRecordListField(this,PersonBson)
  object licenses extends BsonRecordListField(this,LicenseBson)
  object keywords extends MongoListField[Module,String](this)
  object dependencies extends MongoMapField[Module, String](this)
  object homepage extends StringField(this, 1024)
  object repository extends StringField(this,1024)
}
object Module extends Module with MongoMetaRecord[Module] {
}

class PersonBson extends BsonRecord[PersonBson] {
  def meta = PersonBson
  object name extends StringField(this,255)
  object url extends StringField(this,1024)
  object email extends StringField(this,255)
}
object PersonBson extends PersonBson with BsonMetaRecord[PersonBson] {
}

class LicenseBson extends BsonRecord[LicenseBson] {
  def meta = LicenseBson
  object licenseType extends StringField(this,255)
  object url extends StringField(this,1024)
}
object LicenseBson extends LicenseBson with BsonMetaRecord[LicenseBson] {
}