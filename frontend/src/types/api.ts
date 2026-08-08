/**
 * TypeScript mirrors of the backend's Java records.
 *
 * Kept deliberately hand-written rather than generated: the surface is small, and the explicit
 * `| null` markers below record which fields the backend genuinely leaves unset, which a naive
 * generator would flatten into optional-everything.
 */

export type Role = 'USER' | 'ADMIN'

/** UUID and Instant/LocalDateTime both arrive as strings over JSON. */
export type Uuid = string
export type IsoDateTime = string

// ---------------------------------------------------------------- auth & user

export interface AuthResponse {
  token: string
}

export interface UserDto {
  id: Uuid
  email: string
  username: string
  defaultDataCenter: string
  defaultWorld: string
  role: Role
  createdAt: IsoDateTime
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  defaultDataCenter: string
  defaultWorld: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface UpdateUserRequest {
  defaultDataCenter: string
  defaultWorld: string
}

// ---------------------------------------------------------------- game data

export interface ItemDto {
  id: Uuid
  xivapiId: number
  name: string
  /** Null for items XIVAPI has no icon for. */
  iconUrl: string | null
  canBeCrafted: boolean
}

export interface RecipeMaterialsDto {
  id: Uuid
  item: ItemDto
  quantity: number
}

export interface RecipeDto {
  id: Uuid
  xivapiId: number
  resultQuantity: number
  job: string
  level: number
  resultItem: ItemDto
  materials: RecipeMaterialsDto[]
}

export interface RecipeSummaryDto {
  id: Uuid
  level: number
  job: string
  resultItemName: string
  resultItemIconUrl: string | null
}

export interface WorldDto {
  name: string
  universalisId: number
  dataCenter: string
  region: string
}

export interface DataCenterDto {
  name: string
  region: string
}

// ---------------------------------------------------------------- craft cost

export type Decision = 'BUY' | 'CRAFT' | 'UNOBTAINABLE' | 'CYCLE'

/** Pricing preference. CHEAPEST is the default and picks whichever of NQ/HQ is listed lower. */
export type Quality = 'CHEAPEST' | 'NQ' | 'HQ'

export interface CraftCostNode {
  itemXivapiId: number
  itemName: string
  quantityNeeded: number
  /** Null when the item is not listed on the market board for the chosen scope. */
  buyCost: number | null
  /** Null when the item has no recipe, or its recipe is itself unobtainable. */
  craftCost: number | null
  /** Null means unobtainable - mirrors the backend's isObtainable(). */
  effectiveCost: number | null
  decision: Decision
  craftsRequired: number
  /** Leftovers when a recipe yields more than the quantity needed. */
  surplus: number
  /** Universalis world id the buyCost came from; resolve to a name via /worlds. */
  cheapestWorldId: number | null
  /** Both qualities, so the UI can show the one that was not chosen. */
  buyCostNq: number | null
  buyCostHq: number | null
  /** Which quality buyCost came from. Null when nothing is listed. */
  buyQuality: 'NQ' | 'HQ' | null
  /** Craft type and level of this item's recipe. Both null when the item has no recipe. */
  job: string | null
  level: number | null
  ingredients: CraftCostNode[]
}

// ---------------------------------------------------------------- saved crafts

export interface SavedCraftRecipeDto {
  recipe: RecipeDto
  quantity: number
}

export interface SavedCraftRecipeRequest {
  recipeId: Uuid
  quantity: number
}

export interface SavedCraftDto {
  id: Uuid
  dataCenter: string
  /** Optional on the backend - a craft priced across a whole data center has no single world. */
  world: string | null
  priceScope: string
  notes: string | null
  title: string
  recipes: SavedCraftRecipeDto[]
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

export interface SavedCraftSummaryDto {
  id: Uuid
  dataCenter: string
  world: string | null
  priceScope: string
  notes: string | null
  title: string
  recipeCount: number
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

export interface SavedCraftCostDto {
  savedCraftId: Uuid
  title: string
  scope: string
  totalCraftCost: number | null
  totalBuyCost: number | null
  savings: number | null
  unobtainableItems: string[]
  items: CraftCostNode[]
}

export interface CreateSavedCraftRequest {
  dataCenter: string
  world?: string | null
  notes?: string | null
  title: string
  recipes: SavedCraftRecipeRequest[]
}

export interface UpdateSavedCraftRequest {
  dataCenter: string
  world?: string | null
  notes?: string | null
  title: string
}

export interface AddRecipeRequest {
  recipes: SavedCraftRecipeRequest[]
}

export interface RemoveRecipeRequest {
  recipeIds: Uuid[]
}

// ---------------------------------------------------------------- admin & errors

export interface SyncStatus {
  running: boolean
  syncedCount: number
  startedAt: IsoDateTime | null
  finishedAt: IsoDateTime | null
}

export interface GameServerSyncResult {
  dataCentersSynced: number
  worldsSynced: number
  worldsSkipped: number
}

export interface FieldError {
  field: string
  message: string
}

/** The shape GlobalExceptionHandler returns for every handled failure. */
export interface ErrorResponse {
  status: number
  message: string
  timeStamp: IsoDateTime
  errors: FieldError[]
}
