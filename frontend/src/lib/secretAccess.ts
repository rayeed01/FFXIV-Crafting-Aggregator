import type { UserDto } from '@/types/api'

/**
 * Who may see the private page, as the UUID from GET /users/me.
 *
 * This is the single place to change it. The value is needed by the page itself, by the header
 * that shows its tab, and by the login redirect, and three copies of one UUID would drift.
 *
 * Keyed on the id rather than the username because the id is immutable, where a username is the
 * kind of thing that gets changed later and silently breaks the check.
 *
 * Empty means nobody: {@link canSeeSecretPage} returns false for every user, so an unfilled
 * constant closes the page rather than opening it to everyone.
 */
export const SECRET_PAGE_USER_ID = '8832f4dc-d079-41bc-b893-a6d0d56ceac8'

/** Route for the page. Changing it here updates the tab and the post-login landing too. */
export const SECRET_PAGE_PATH = '/secret'

/** Tab label in the header. */
export const SECRET_PAGE_LABEL = 'Secret'

/**
 * Whether this user may see the private page.
 *
 * Presentational only. The image is a static file and stays reachable by anyone who knows its
 * URL - the route and the tab are hidden, the file is not.
 */
export function canSeeSecretPage(user: UserDto | null | undefined): boolean {
  return Boolean(SECRET_PAGE_USER_ID) && user?.id === SECRET_PAGE_USER_ID
}

/**
 * Where a user should land after signing in.
 *
 * The private page for the one account it belongs to, and Browse for everyone else. A page the
 * user was actually trying to reach takes precedence over both - see LoginPage.
 */
export function landingPathFor(user: UserDto | null | undefined): string {
  return canSeeSecretPage(user) ? SECRET_PAGE_PATH : '/search'
}
