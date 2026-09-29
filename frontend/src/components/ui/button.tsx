import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../../lib/utils'
const variants = cva('inline-flex items-center justify-center gap-2 rounded-lg text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500', { variants: { variant: { default: 'bg-emerald-800 text-white hover:bg-emerald-900', secondary: 'bg-stone-100 text-stone-800 hover:bg-stone-200', outline: 'border border-stone-200 bg-white hover:bg-stone-50', ghost: 'hover:bg-stone-100', destructive: 'bg-red-600 text-white hover:bg-red-700' }, size: { default: 'h-10 px-4 py-2', sm: 'h-8 px-3', icon: 'h-9 w-9' } }, defaultVariants: { variant: 'default', size: 'default' } })
export function Button({ className, variant, size, asChild = false, ...props }: React.ComponentProps<'button'> & VariantProps<typeof variants> & { asChild?: boolean }) { const Comp = asChild ? Slot : 'button'; return <Comp className={cn(variants({ variant, size }), className)} {...props} /> }
