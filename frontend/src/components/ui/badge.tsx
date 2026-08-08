import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium transition-colors',
  {
    variants: {
      variant: {
        default: 'border-transparent bg-primary text-primary-foreground',
        secondary: 'border-transparent bg-secondary text-secondary-foreground',
        outline: 'border-border text-foreground',
        /* Decision colours. Each pairs a tinted ground with a saturated foreground rather than
           relying on hue alone, so BUY/CRAFT stay distinguishable without colour vision. */
        buy: 'border-transparent bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-300',
        craft: 'border-transparent bg-indigo-100 text-indigo-800 dark:bg-indigo-950 dark:text-indigo-300',
        unobtainable: 'border-transparent bg-zinc-200 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300',
        cycle: 'border-transparent bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-300',
        success: 'border-transparent bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300',
        destructive: 'border-transparent bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300',
      },
    },
    defaultVariants: { variant: 'default' },
  },
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />
}

export { badgeVariants }
