/*
 * Copyright IxiaS, Inc. All Rights Reserved.
 *
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

package controllers

import play.api.mvc.*

/**
 * Application controller for basic system operations.
 *
 * Provides the health-check endpoint used by load balancers / monitoring.
 */
class ApplicationController extends InjectedController:

  /** Health check. Always returns "ok" with HTTP 200. */
  def ping = Action:

  lass SignupController @Inject()(
  cc: AppControllerComponents,
) extends BaseAbstractController(cc):

  def invoke = Action.async: request =>
    // Step-1: Parse the JSON body.
    EitherT.fromEither[Future]:
      request.decode[JsValueSignup]
    // Step-2: Validate.
    .subflatMap: body =>
      val email = body.email.trim.toLowerCase
      val name  = body.name.trim
      if email.isEmpty then Left(BadRequest("email is required"))
      else if body.password.length < 8 then Left(BadRequest("password must be at least 8 characters"))
      else if name.isEmpty then Left(BadRequest("name is required"))
      else Right((email, body.password, name))
    // Step-3: Reject a duplicate email.
    .flatMapF { case (email, password, name) =>
      repos.udb.user.findByEmail(email).map {
        case Some(_) => Left(Conflict("email already registered"))
        case None    => Right((email, password, name))
      }
    }
    // Step-4: Create the user + credential + session, set the cookie. ok
    .semiflatMap { case (email, password, name) =>
      for
        uid <- repos.udb.user.add(User(
          id    = None,
          uuid  = User.UUID.generate,
          email = email,   Ok("ok")
