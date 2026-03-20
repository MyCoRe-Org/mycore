<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import type { CommandEntry, CommandGroup } from '@/types';

const MIN_VISIBLE_COMMANDS = 10;

const props = defineProps<{
  groups: CommandGroup[];
}>();

const emit = defineEmits<{
  select: [value: string];
}>();

const isMenuOpen = ref(false);
const openGroupIndex = ref<number | null>(null);
const rootRef = ref<HTMLElement | null>(null);
const toggleButtonRef = ref<HTMLButtonElement | null>(null);

function onDocumentClick(event: MouseEvent): void {
  const target = event.target as HTMLElement | null;
  if (!target?.closest('.webcli-command-menu')) {
    closeMenu();
  }
}

function closeMenu(): void {
  isMenuOpen.value = false;
  openGroupIndex.value = null;
}

function toggleMenu(): void {
  isMenuOpen.value = !isMenuOpen.value;
  if (!isMenuOpen.value) {
    openGroupIndex.value = null;
  }
}

function getGroupButtons(): HTMLButtonElement[] {
  return Array.from(rootRef.value?.querySelectorAll<HTMLButtonElement>('[data-group-button]') ?? []);
}

function getCommandButtons(groupIndex: number): HTMLButtonElement[] {
  return Array.from(
    rootRef.value?.querySelectorAll<HTMLButtonElement>(`[data-command-button="${groupIndex}"]`) ?? []
  );
}

function focusGroupButton(groupIndex: number): void {
  getGroupButtons().at(groupIndex)?.focus();
}

function focusCommandButton(groupIndex: number, commandIndex: number): void {
  getCommandButtons(groupIndex).at(commandIndex)?.focus();
}

function positionSubmenu(parent: HTMLElement, commands: CommandEntry[]): void {
  const submenu = parent.querySelector('.dropdown-menu');
  if (!(submenu instanceof HTMLElement)) {
    return;
  }
  let itemHeight = 32;
  const firstItem = submenu.querySelector('.dropdown-item');
  if (firstItem instanceof HTMLElement && firstItem.offsetHeight > 0) {
    itemHeight = firstItem.offsetHeight;
  }
  const minRequiredItems = Math.min(commands.length, MIN_VISIBLE_COMMANDS);
  const minRequiredHeight = minRequiredItems * itemHeight + 10;
  const parentRect = parent.getBoundingClientRect();
  const availableSpaceBelow = window.innerHeight - parentRect.top - 10;
  let newTop = parentRect.top;
  if (availableSpaceBelow < minRequiredHeight) {
    const neededShift = minRequiredHeight - availableSpaceBelow;
    newTop = Math.max(10, parentRect.top - neededShift);
  }
  submenu.style.maxHeight = `${window.innerHeight - newTop - 10}px`;
  submenu.style.top = `${newTop}px`;
  submenu.style.left = `${parentRect.right}px`;
}

function openGroup(groupIndex: number): void {
  isMenuOpen.value = true;
  openGroupIndex.value = groupIndex;
}

function onGroupMouseenter(event: MouseEvent, groupIndex: number, commands: CommandEntry[]): void {
  const parent = event.currentTarget;
  if (!(parent instanceof HTMLElement)) {
    return;
  }
  openGroup(groupIndex);
  positionSubmenu(parent, commands);
}

function onGroupFocus(event: FocusEvent, groupIndex: number, commands: CommandEntry[]): void {
  const currentTarget = event.currentTarget;
  if (!(currentTarget instanceof HTMLElement)) {
    return;
  }
  openGroup(groupIndex);
  positionSubmenu(currentTarget.parentElement ?? currentTarget, commands);
}

function onToggleKeydown(event: KeyboardEvent): void {
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    isMenuOpen.value = true;
    openGroup(0);
    nextTick(() => focusGroupButton(0));
  }
  if (event.key === 'Escape') {
    closeMenu();
  }
}

function onGroupKeydown(event: KeyboardEvent, groupIndex: number, commands: CommandEntry[]): void {
  const groupCount = props.groups.length;
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    const nextIndex = (groupIndex + 1) % groupCount;
    openGroup(nextIndex);
    nextTick(() => focusGroupButton(nextIndex));
    return;
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault();
    const nextIndex = (groupIndex - 1 + groupCount) % groupCount;
    openGroup(nextIndex);
    nextTick(() => focusGroupButton(nextIndex));
    return;
  }
  if (event.key === 'ArrowRight' || event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    openGroup(groupIndex);
    nextTick(() => focusCommandButton(groupIndex, 0));
    return;
  }
  if (event.key === 'Escape' || event.key === 'ArrowLeft') {
    event.preventDefault();
    closeMenu();
    toggleButtonRef.value?.focus();
    return;
  }
  if (event.key === 'Tab') {
    closeMenu();
    return;
  }
  const currentTarget = event.currentTarget;
  if (currentTarget instanceof HTMLElement) {
    positionSubmenu(currentTarget.parentElement ?? currentTarget, commands);
  }
}

function onCommandKeydown(event: KeyboardEvent, groupIndex: number, commandIndex: number): void {
  const groupCommands = props.groups[groupIndex]?.commands ?? [];
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    const nextIndex = (commandIndex + 1) % groupCommands.length;
    focusCommandButton(groupIndex, nextIndex);
    return;
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault();
    const nextIndex = (commandIndex - 1 + groupCommands.length) % groupCommands.length;
    focusCommandButton(groupIndex, nextIndex);
    return;
  }
  if (event.key === 'ArrowLeft') {
    event.preventDefault();
    focusGroupButton(groupIndex);
    return;
  }
  if (event.key === 'Escape') {
    event.preventDefault();
    closeMenu();
    toggleButtonRef.value?.focus();
  }
}

function selectCommand(command: string): void {
  emit('select', command);
  closeMenu();
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick);
});
</script>

<template>
  <li ref="rootRef" class="nav-item dropdown webcli-command-menu">
    <button
      ref="toggleButtonRef"
      class="nav-link dropdown-toggle btn btn-link"
      type="button"
      data-bs-display="static"
      aria-haspopup="true"
      :aria-expanded="isMenuOpen"
      aria-controls="webcli-command-groups"
      @click="toggleMenu"
      @keydown="onToggleKeydown"
    >
      Command
    </button>
    <ul id="webcli-command-groups" class="dropdown-menu" :class="{ show: isMenuOpen }" role="menu">
      <li
        v-for="(group, groupIndex) in groups"
        :key="group.name"
        class="dropdown-submenu"
        :class="{ show: openGroupIndex === groupIndex }"
        @mouseenter="onGroupMouseenter($event, groupIndex, group.commands)"
      >
        <button
          class="dropdown-item"
          type="button"
          role="menuitem"
          :aria-expanded="openGroupIndex === groupIndex"
          :aria-controls="`webcli-command-group-${groupIndex}`"
          data-group-button
          @focus="onGroupFocus($event, groupIndex, group.commands)"
          @keydown="onGroupKeydown($event, groupIndex, group.commands)"
        >
          {{ group.name }}
        </button>
        <ul
          :id="`webcli-command-group-${groupIndex}`"
          class="dropdown-menu"
          :class="{ show: openGroupIndex === groupIndex }"
          role="menu"
        >
          <li v-for="(entry, commandIndex) in group.commands" :key="entry.command">
            <button
              class="dropdown-item"
              type="button"
              role="menuitem"
              :title="entry.help"
              :data-command-button="groupIndex"
              @click="selectCommand(entry.command)"
              @keydown="onCommandKeydown($event, groupIndex, commandIndex)"
            >
              {{ entry.command }}
            </button>
          </li>
        </ul>
      </li>
    </ul>
  </li>
</template>
