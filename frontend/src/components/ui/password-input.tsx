import * as React from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

/**
 * Password field with a reveal toggle.
 *
 * Defaults to hidden and never persists the revealed state, so the shoulder-surfing protection
 * is intact unless the user deliberately asks to see what they typed.
 */
export const PasswordInput = React.forwardRef<
  HTMLInputElement,
  React.InputHTMLAttributes<HTMLInputElement>
>(({ className, ...props }, ref) => {
  const [visible, setVisible] = React.useState(false)

  return (
    <div className="relative">
      <Input
        ref={ref}
        type={visible ? 'text' : 'password'}
        className={cn('pr-10', className)}
        {...props}
      />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        // tabIndex -1 keeps Tab going straight from the field to submit, which is what someone
        // typing a password expects; the button stays reachable by mouse and by screen readers.
        tabIndex={-1}
        aria-label={visible ? 'Hide password' : 'Show password'}
        title={visible ? 'Hide password' : 'Show password'}
        className="absolute right-0 top-0 grid h-9 w-10 place-items-center rounded-r-md text-muted-foreground transition-colors hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
      </button>
    </div>
  )
})
PasswordInput.displayName = 'PasswordInput'
