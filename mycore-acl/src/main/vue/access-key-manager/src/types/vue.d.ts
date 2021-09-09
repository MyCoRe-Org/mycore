// 1. Make sure to import 'vue' before declaring augmented types
import MCRAccessKeyServicePlugin from '@/plugins/MCRAccessKeyServicePlugin';

// 2. Specify a file with the types you want to augment
//    Vue has the constructor type in types/vue.d.ts
declare module 'vue/types/vue' {
  // 3. Declare augmentation for Vue
  interface Vue {
    $t: function;
    $client: MCRAccessKeyServicePlugin;
  }
}
