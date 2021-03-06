package com.carsecurity.authorization.controller

import com.carsecurity.authorization.domain.Role
import com.carsecurity.authorization.domain.dto.RoleDTO
import com.carsecurity.authorization.service.RoleService
import com.carsecurity.authorization.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller used for managing users roles. Only user with role SUPER_ADMIN can access this controllers.
 *
 * @param userService is service for access users in database.
 * @param roleService is service for access roles in database.
 */
@RestController
@RequestMapping("role")
class RoleController(
        private val userService: UserService,
        private val roleService: RoleService

) {
    /** Logger of this class. */
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Method returns list of all available roles in database.
     * @return list of [RoleDTO] which can be append to user.
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun getRole(): List<RoleDTO> = roleService.getRoles().map { role -> RoleDTO(role) }

    /**
     * Method create new role in database according to given role. When role can not be created http return code is
     * BAD_REQUEST.
     *
     * @param roleCreate is role which will be created in database.
     * @return created role from database.
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun createRole(@RequestBody roleCreate: RoleDTO): ResponseEntity<RoleDTO> {

        roleCreate.id = 0
        val users = userService.findAllByUsername(roleCreate.users.toList())
        if (users.size != roleCreate.users.size) {
            logger.debug("Can not save role because users does not exists.")
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        val role = Role(
                id = roleCreate.id,
                name = roleCreate.name,
                users = users.toHashSet()
        )

        val roleOptional = roleService.tryCreate(role)

        return if (roleOptional.isPresent) {
            ResponseEntity(RoleDTO(roleOptional.get()), HttpStatus.CREATED)
        } else {
            logger.debug("Role can not be created.")
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    /**
     * This method is used for updating existing role in database. [roleUpdate] must contains identification number of
     * role in database. When role can not be updated http status code BAD_REQUEST will be returned.
     *
     * @param roleUpdate is role which will be updated in database.
     * @return update role from database.
     */
    @PutMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun updateRole(@RequestBody roleUpdate: RoleDTO): ResponseEntity<RoleDTO> {

        // check if id exists
        val roleToUpdate = roleService.findById(roleUpdate.id)
        if (!roleToUpdate.isPresent) {
            logger.debug("Role to update does not exists.")
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        // if name different -> check if username exists
        if (roleToUpdate.get().name != roleUpdate.name) {
            val roleWithName = roleService.findByName(roleUpdate.name)
            if (roleWithName.isPresent) {
                logger.debug("Role with the same name already exists.")
                return ResponseEntity(HttpStatus.BAD_REQUEST)
            }
        }

        // check if users exists
        val users = userService.findAllByUsername(roleUpdate.users.toList())
        if (users.size != roleUpdate.users.size) {
            logger.debug("Can not update role because users does not exists.")
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        // store to db
        val resultRole = roleService.update(Role(id = roleUpdate.id, name = roleUpdate.name, users = users.toHashSet()))

        return if (resultRole.isPresent) {
            ResponseEntity.ok(RoleDTO(resultRole.get()))
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    /**
     * Method delete role from database.
     * @param roleId identification of deleted role.
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun deleteRole(@RequestParam(name = "id") roleId: Long) {
        roleService.deleteById(roleId)
    }
}