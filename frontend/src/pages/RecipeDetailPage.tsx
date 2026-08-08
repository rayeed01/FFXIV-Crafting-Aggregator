import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Calculator } from 'lucide-react'
import { api } from '@/lib/api'
import { useAsync } from '@/hooks/useAsync'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ItemIcon } from '@/components/ItemIcon'
import { jobName } from '@/lib/jobs'
import { ErrorState, PageHeader } from '@/components/states'
import { formatNumber } from '@/lib/format'

export function RecipeDetailPage() {
  const { recipeId } = useParams<{ recipeId: string }>()
  const navigate = useNavigate()
  const location = useLocation()

  const { data: recipe, error, loading, reload } = useAsync(
    (signal) => api.recipes.byId(recipeId!, signal),
    [recipeId],
    Boolean(recipeId),
  )

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (error) return <ErrorState error={error} onRetry={reload} />
  if (!recipe) return null

  return (
    <div className="space-y-6">
      {/* A hardcoded /search link discarded whatever had been typed. Going back through history
          returns to the exact search URL instead, query and all. location.key is "default" only
          on a directly-loaded page, where there is no history entry to return to. */}
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2"
        onClick={() => (location.key === 'default' ? navigate('/search') : navigate(-1))}
      >
        <ArrowLeft /> Back to search
      </Button>

      <PageHeader
        title={recipe.resultItem.name}
        description={[
          jobName(recipe.job),
          recipe.level > 0 ? `Level ${recipe.level}` : null,
          `Yields ${formatNumber(recipe.resultQuantity)}`,
        ]
          .filter(Boolean)
          .join(' · ')}
        actions={
          <Button asChild>
            <Link to={`/craft-cost/${recipe.resultItem.xivapiId}`}>
              <Calculator /> Calculate cost
            </Link>
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">
            Materials <span className="text-muted-foreground">({recipe.materials.length})</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <ul className="divide-y divide-border border-t border-border">
            {recipe.materials.map((material) => (
              <li key={material.id} className="flex items-center gap-3 px-5 py-3">
                <ItemIcon src={material.item.iconUrl} alt={material.item.name} className="size-8" />
                <span className="min-w-0 flex-1 truncate">{material.item.name}</span>
                {material.item.canBeCrafted && <Badge variant="craft">Craftable</Badge>}
                <span className="tabular w-12 shrink-0 text-right font-medium">
                  ×{formatNumber(material.quantity)}
                </span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  )
}
