import { useSyncExternalStore } from 'react';
import { getTheme, subscribeTheme } from '@/utils/theme';

/**
 * Tema activo ("light" o "dark"). Se actualiza cuando la persona lo cambia o, si no eligió,
 * cuando el sistema operativo cambia de modo. Para cambiarlo, usar toggleTheme de utils/theme.
 * @returns {"light"|"dark"}
 */
export function useTheme() {
    return useSyncExternalStore(subscribeTheme, getTheme, () => 'light');
}