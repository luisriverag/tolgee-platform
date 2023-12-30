package io.tolgee.activity

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.data.EntityDescription
import io.tolgee.activity.data.EntityDescriptionWithRelations
import io.tolgee.model.EntityWithId
import io.tolgee.util.EntityUtil
import org.springframework.stereotype.Component
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Component
class EntityDescriptionProvider(
  private val entityUtil: EntityUtil,
) {
  fun getDescriptionWithRelations(entity: EntityWithId): EntityDescriptionWithRelations? {
    val description = getDescription(entity) ?: return null

    val relations = mutableMapOf<String, EntityDescriptionWithRelations>()

    entity::class.findAnnotation<ActivityEntityDescribingPaths>()?.paths?.forEach pathsForEach@{ path ->
      var realDescribingEntity: Any = entity
      path.split(".").forEach { pathItem ->
        val member = realDescribingEntity::class.members.find { it.name == pathItem }
        realDescribingEntity = member?.call(realDescribingEntity) ?: return@pathsForEach
      }

      (realDescribingEntity as? EntityWithId)?.let {
        relations[path] = getDescriptionWithRelations(it) ?: return@pathsForEach
      }
    }

    return EntityDescriptionWithRelations(
      description.entityClass,
      description.entityId,
      description.data,
      relations,
    )
  }

  fun getDescription(entity: EntityWithId): EntityDescription? {
    val entityClass = entityUtil.getRealEntityClass(entity::class.java) ?: return null

    val fieldValues =
      entityClass.kotlin.members.filter { member ->
        member.hasAnnotation<ActivityDescribingProp>()
      }.associateTo(HashMap()) { it.name to it.call(entity) }

    return EntityDescription(
      entityClass.simpleName,
      entityId = entity.id,
      fieldValues,
    )
  }
}
