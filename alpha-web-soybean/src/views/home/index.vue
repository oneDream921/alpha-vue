<script setup lang="ts">
import { computed } from 'vue';
import { DatabaseOutlined, FileOutlined, SettingOutlined, TeamOutlined } from '@ant-design/icons-vue';
import logoUrl from '@/assets/alpha-logo.svg';
import { authStore } from '@/stores/auth';

const displayName = computed(() => authStore.state.profile?.nickname || authStore.state.profile?.username || '管理员');

const quickLinks = computed(() =>
  [
    {
      title: '用户管理',
      description: '维护账号、角色与登录状态',
      path: '/system/users',
      permission: 'system:user:list',
      icon: TeamOutlined
    },
    {
      title: '系统配置',
      description: '管理站点、存储与安全参数',
      path: '/system/settings',
      permission: 'system:setting:list',
      icon: SettingOutlined
    },
    {
      title: 'Redis 管理',
      description: '查看缓存概览与运行指标',
      path: '/monitor/redis',
      permission: 'monitor:redis:list',
      icon: DatabaseOutlined
    },
    {
      title: '文件管理',
      description: '浏览和维护平台文件资源',
      path: '/files',
      permission: 'file:list',
      icon: FileOutlined
    }
  ].filter(item => authStore.hasPermission(item.permission))
);
</script>

<template>
  <section class="page-section dashboard-page">
    <section class="dashboard-hero" aria-labelledby="dashboard-title">
      <div class="dashboard-hero-copy">
        <span class="dashboard-eyebrow">ALPHA VUE 管理平台</span>
        <h1 id="dashboard-title">欢迎回来，{{ displayName }}</h1>
        <p>从常用入口开始，快速管理你的业务与系统资源。</p>
      </div>
      <div class="dashboard-hero-art" aria-hidden="true">
        <img :src="logoUrl" alt="" />
      </div>
      <div class="dashboard-summary">
        <div>
          <span>当前会话</span>
          <strong>安全连接</strong>
        </div>
        <div>
          <span>可访问模块</span>
          <strong>{{ quickLinks.length }} 个</strong>
        </div>
      </div>
    </section>
    <div class="dashboard-section-heading">
      <div>
        <h2>常用入口</h2>
        <p>根据当前权限为你展示可用功能。</p>
      </div>
    </div>
    <div v-if="quickLinks.length" class="dashboard-grid">
      <RouterLink v-for="link in quickLinks" :key="link.path" :to="link.path" class="quick-link">
        <span class="quick-link-icon"><component :is="link.icon" /></span>
        <span class="quick-link-content">
          <strong>{{ link.title }}</strong>
          <span>{{ link.description }}</span>
        </span>
        <span class="quick-link-arrow" aria-hidden="true">→</span>
      </RouterLink>
    </div>
    <AEmpty v-else description="暂无可访问的功能" />
  </section>
</template>

<style scoped>
.dashboard-page {
  min-height: 100%;
  padding: 24px;
  box-sizing: border-box;
  background: var(--alpha-canvas);
}

.dashboard-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  min-height: 220px;
  overflow: hidden;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: linear-gradient(135deg, var(--alpha-surface) 0%, var(--alpha-canvas) 100%);
  box-shadow: var(--alpha-shadow);
}

.dashboard-hero::after {
  position: absolute;
  right: -100px;
  bottom: -160px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: var(--alpha-primary-soft);
  content: '';
}

.dashboard-hero-copy {
  position: relative;
  z-index: 1;
  align-self: center;
  padding: 32px 0 76px 36px;
}

.dashboard-eyebrow {
  color: var(--alpha-primary-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.dashboard-hero h1 {
  margin: 12px 0 8px;
  color: var(--alpha-text-primary);
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 600;
  line-height: 1.25;
}

.dashboard-hero-copy p {
  margin: 0;
  color: var(--alpha-text-secondary);
  font-size: 14px;
}

.dashboard-hero-art {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dashboard-hero-art img {
  width: 150px;
  height: 150px;
  object-fit: contain;
  opacity: 0.92;
}

.dashboard-summary {
  position: absolute;
  left: 36px;
  bottom: 24px;
  z-index: 2;
  display: flex;
  gap: 24px;
}

.dashboard-summary div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dashboard-summary span {
  color: var(--alpha-muted);
  font-size: 12px;
}

.dashboard-summary strong {
  color: var(--alpha-text);
  font-size: 14px;
}

.dashboard-section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin: 28px 0 14px;
}

.dashboard-section-heading h2 {
  margin: 0 0 5px;
  color: var(--alpha-text-primary);
  font-size: 20px;
  font-weight: 600;
}

.dashboard-section-heading p {
  margin: 0;
  color: var(--alpha-muted);
  font-size: 13px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.quick-link {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 92px;
  padding: 18px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: var(--alpha-surface);
  color: inherit;
  text-decoration: none;
  box-shadow: var(--alpha-shadow);
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.quick-link:hover {
  border-color: var(--alpha-primary);
  box-shadow: var(--alpha-shadow);
  transform: translateY(-2px);
}

.quick-link-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  margin-right: 12px;
  border-radius: 9px;
  background: var(--alpha-primary-soft);
  color: var(--alpha-primary-strong);
  font-size: 20px;
}

.quick-link-content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 5px;
}

.quick-link-content strong {
  overflow: hidden;
  color: var(--alpha-text);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-link-content span {
  overflow: hidden;
  color: var(--alpha-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-link-arrow {
  margin-left: 10px;
  color: var(--alpha-muted);
  font-size: 18px;
  transition:
    color 0.2s,
    transform 0.2s;
}

.quick-link:hover .quick-link-arrow {
  color: var(--alpha-primary-strong);
  transform: translateX(3px);
}

@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .dashboard-page {
    padding: 16px;
  }

  .dashboard-hero {
    grid-template-columns: 1fr;
    min-height: 260px;
  }

  .dashboard-hero-copy {
    padding: 26px 24px 0;
  }

  .dashboard-hero-art {
    position: absolute;
    right: 10px;
    bottom: 20px;
  }

  .dashboard-hero-art img {
    width: 100px;
    height: 100px;
  }

  .dashboard-summary {
    right: auto;
    bottom: 24px;
    left: 24px;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
