import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { SearchPage } from '@/pages/SearchPage'
import { RecipeDetailPage } from '@/pages/RecipeDetailPage'
import { CraftCostPage } from '@/pages/CraftCostPage'
import { SavedCraftsPage } from '@/pages/SavedCraftsPage'
import { SavedCraftDetailPage } from '@/pages/SavedCraftDetailPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { SecretPage } from '@/pages/SecretPage'
import { AdminPage } from '@/pages/AdminPage'
import { NotFoundPage } from '@/pages/NotFoundPage'

/** Carries the id across the /saved-crafts -> /lists rename. */
function LegacySavedCraftRedirect() {
  const { savedCraftId } = useParams<{ savedCraftId: string }>()
  return <Navigate to={`/lists/${savedCraftId}`} replace />
}

/**
 * Route table.
 *
 * Browse, recipes and craft cost are public, mirroring the backend's permitAll on items, recipes
 * and craft-cost. Lists and profile require a session; admin additionally requires the role.
 *
 * The /saved-crafts paths are the pre-rename URLs, kept as redirects so existing links and
 * bookmarks continue to resolve.
 */
export function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/search" replace />} />

        <Route path="/search" element={<SearchPage />} />
        <Route path="/recipes/:recipeId" element={<RecipeDetailPage />} />
        <Route path="/craft-cost" element={<CraftCostPage />} />
        <Route path="/craft-cost/:itemXivapiId" element={<CraftCostPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/lists" element={<SavedCraftsPage />} />
          <Route path="/lists/:savedCraftId" element={<SavedCraftDetailPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          {/* Deliberately absent from the navigation; SecretPage gates on the account itself. */}
          <Route path="/secret" element={<SecretPage />} />
        </Route>

        <Route path="/saved-crafts" element={<Navigate to="/lists" replace />} />
        <Route path="/saved-crafts/:savedCraftId" element={<LegacySavedCraftRedirect />} />

        <Route element={<ProtectedRoute requireAdmin />}>
          <Route path="/admin" element={<AdminPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
