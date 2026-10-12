import { useQuery } from '@tanstack/react-query';
import { api } from '../shared/api';
import type { UiConfig } from './UiModule';

/** Loads public/config.json once. */
export function useConfig() {
  return useQuery({
    queryKey: ['ui-config'],
    queryFn: () => api.get<UiConfig>('/config.json'),
    staleTime: Infinity,
  });
}
