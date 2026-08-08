import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center gap-4 py-24 text-center">
      <p className="text-5xl font-semibold tracking-tight text-muted-foreground">404</p>
      <div className="space-y-1">
        <h1 className="text-xl font-semibold">Page not found</h1>
        <p className="text-sm text-muted-foreground">That route does not exist.</p>
      </div>
      <Button asChild>
        <Link to="/search">Back to browse</Link>
      </Button>
    </div>
  )
}
