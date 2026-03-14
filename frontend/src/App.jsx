import { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'insight_auth_token'
const USER_KEY = 'insight_auth_user'
const THEME_KEY = 'insight_theme'
const EMPTY_REPOSITORY_FORM = { owner: '', repositoryName: '', alias: '', notes: '' }
const EMPTY_LOGIN_FORM = { email: '', password: '' }
const EMPTY_REGISTER_FORM = { fullName: '', email: '', password: '' }
const NAV_ITEMS = ['Overview', 'Repositories', 'Analytics', 'AI Insight']
const LANGUAGE_META = {
  Java: { icon: 'J', color: '#f97316' },
  JavaScript: { icon: 'JS', color: '#facc15' },
  TypeScript: { icon: 'TS', color: '#38bdf8' },
  Python: { icon: 'Py', color: '#22c55e' },
  'C++': { icon: 'C++', color: '#a78bfa' },
  Go: { icon: 'Go', color: '#14b8a6' },
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || '')
  const [currentUser, setCurrentUser] = useState(() => {
    const stored = localStorage.getItem(USER_KEY)
    return stored ? JSON.parse(stored) : null
  })
  const [theme, setTheme] = useState(() => localStorage.getItem(THEME_KEY) || 'dark')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  const handleAuthenticated = useCallback((payload) => {
    setToken(payload.token)
    setCurrentUser(payload.user)
    localStorage.setItem(TOKEN_KEY, payload.token)
    localStorage.setItem(USER_KEY, JSON.stringify(payload.user))
  }, [])

  const handleLogout = useCallback(() => {
    setToken('')
    setCurrentUser(null)
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }, [])

  return (
    <Routes>
      <Route path="/login" element={token ? <Navigate to="/" replace /> : <AuthPage mode="login" onAuthenticated={handleAuthenticated} theme={theme} setTheme={setTheme} />} />
      <Route path="/register" element={token ? <Navigate to="/" replace /> : <AuthPage mode="register" onAuthenticated={handleAuthenticated} theme={theme} setTheme={setTheme} />} />
      <Route path="/" element={token ? <Dashboard token={token} currentUser={currentUser} onLogout={handleLogout} theme={theme} setTheme={setTheme} /> : <Navigate to="/login" replace />} />
    </Routes>
  )
}

function AuthPage({ mode, onAuthenticated, theme, setTheme }) {
  const navigate = useNavigate()
  const [form, setForm] = useState(mode === 'login' ? EMPTY_LOGIN_FORM : EMPTY_REGISTER_FORM)
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    setForm(mode === 'login' ? EMPTY_LOGIN_FORM : EMPTY_REGISTER_FORM)
    setError('')
  }, [mode])

  function updateForm(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setIsSubmitting(true)
    setError('')
    try {
      const payload = await fetchJson(`/api/auth/${mode}`, { method: 'POST', body: JSON.stringify(form) })
      onAuthenticated(payload)
      navigate('/', { replace: true })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <section className="auth-stage">
        <div className="auth-brand-card floating-panel">
          <div>
            <p className="eyebrow">GitSense</p>
            <h1>GitSense</h1>
            <p className="auth-subtitle">Smart understanding of Git repositories</p>
            <p className="hero-copy">Monitor repository health, compare contributor activity, and generate structured AI summaries from a single protected dashboard.</p>
          </div>
          <div className="auth-metric-grid">
            <MetricTile label="Tracked repos" value="Multi" />
            <MetricTile label="Charts" value="Live" />
            <MetricTile label="AI analysis" value="Ready" />
          </div>
        </div>

        <div className="auth-form-card floating-panel">
          <div className="auth-topbar">
            <div className="auth-tabs">
              <button type="button" className={mode === 'login' ? 'tab active' : 'tab'} onClick={() => navigate('/login')}>Login</button>
              <button type="button" className={mode === 'register' ? 'tab active' : 'tab'} onClick={() => navigate('/register')}>Register</button>
            </div>
            <ThemeToggle theme={theme} setTheme={setTheme} compact />
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            <h2>{mode === 'login' ? 'Sign in' : 'Create account'}</h2>
            {mode === 'register' && <label>Full name<input name="fullName" value={form.fullName || ''} onChange={updateForm} placeholder="Your name" required /></label>}
            <label>Email<input name="email" type="email" value={form.email || ''} onChange={updateForm} placeholder="you@example.com" required /></label>
            <label>Password<input name="password" type="password" value={form.password || ''} onChange={updateForm} placeholder="Minimum 6 characters" required /></label>
            {error && <p className="inline-message error">{error}</p>}
            <button type="submit" className="primary-button" disabled={isSubmitting}>{isSubmitting ? 'Please wait...' : mode === 'login' ? 'Login' : 'Register'}</button>
          </form>
        </div>
      </section>
    </div>
  )
}

function Dashboard({ token, currentUser, onLogout, theme, setTheme }) {
  const [activeView, setActiveView] = useState('Overview')
  const [repositories, setRepositories] = useState([])
  const [summary, setSummary] = useState({ trackedRepositories: 0, totalStars: 0, totalForks: 0, averageWeeklyCommits: 0, totalContributors: 0, healthiestRepository: 'No repositories yet' })
  const [selectedId, setSelectedId] = useState(null)
  const [repositoryForm, setRepositoryForm] = useState(EMPTY_REPOSITORY_FORM)
  const [isEditing, setIsEditing] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [activeAction, setActiveAction] = useState('')
  const [toast, setToast] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [languageFilter, setLanguageFilter] = useState('All')
  const [sortOrder, setSortOrder] = useState('activity')
  const [analysisUrl, setAnalysisUrl] = useState('')
  const [isAnalysisLoading, setIsAnalysisLoading] = useState(false)
  const [analysisError, setAnalysisError] = useState('')

  const selectedRepository = useMemo(() => repositories.find((repository) => repository.id === selectedId) || repositories[0] || null, [repositories, selectedId])
  const availableLanguages = useMemo(() => ['All', ...new Set(repositories.map((repository) => repository.primaryLanguage).filter(Boolean))], [repositories])
  const filteredRepositories = useMemo(() => {
    const query = searchTerm.trim().toLowerCase()
    return [...repositories]
      .filter((repository) => {
        const matchesQuery = !query || [repository.fullName, repository.alias, repository.owner].filter(Boolean).some((value) => value.toLowerCase().includes(query))
        const matchesLanguage = languageFilter === 'All' || repository.primaryLanguage === languageFilter
        return matchesQuery && matchesLanguage
      })
      .sort((left, right) => {
        if (sortOrder === 'activity') return (right.commitVelocity || 0) - (left.commitVelocity || 0)
        if (sortOrder === 'recent') return new Date(right.lastSyncedAt || 0) - new Date(left.lastSyncedAt || 0)
        return (right.stars || 0) - (left.stars || 0)
      })
  }, [repositories, searchTerm, languageFilter, sortOrder])

  const dashboardMetrics = useMemo(() => {
    const totalCommits = repositories.flatMap((repository) => repository.commitTimeline || []).reduce((sum, point) => sum + point.commits, 0)
    const linesAnalyzed = repositories.reduce((sum, repository) => sum + ((repository.languages || []).length * 2400), 0)
    return [
      { label: 'Total repositories tracked', value: summary.trackedRepositories },
      { label: 'Total commits', value: totalCommits },
      { label: 'Active contributors', value: summary.totalContributors },
      { label: 'Lines of code analyzed', value: linesAnalyzed },
    ]
  }, [repositories, summary])

  const topLanguages = useMemo(() => aggregateLanguages(repositories), [repositories])
  const commitSeries = useMemo(() => aggregateCommitTimeline(repositories), [repositories])
  const contributorSeries = useMemo(() => aggregateContributors(repositories), [repositories])
  const trendSeries = useMemo(() => filteredRepositories.slice(0, 6).map((repository) => ({ label: repository.repositoryName, value: Number((repository.commitVelocity || 0).toFixed(1)) })), [filteredRepositories])

  const showToast = useCallback((message, tone = 'success') => {
    setToast({ message, tone })
    window.clearTimeout(showToast.timeoutId)
    showToast.timeoutId = window.setTimeout(() => setToast(null), 3200)
  }, [])

  const loadDashboard = useCallback(async (nextSelectedId) => {
    setIsLoading(true)
    try {
      const [repositoryList, dashboardSummary] = await Promise.all([
        fetchJson('/api/repositories', {}, true, token),
        fetchJson('/api/dashboard/summary', {}, true, token),
      ])
      setRepositories(repositoryList)
      setSummary(dashboardSummary)
      if (repositoryList.length === 0) {
        setSelectedId(null)
      } else if (nextSelectedId) {
        setSelectedId(nextSelectedId)
      } else if (!repositoryList.some((repository) => repository.id === selectedId)) {
        setSelectedId(repositoryList[0].id)
      }
    } catch (requestError) {
      if (requestError.status === 401) {
        onLogout()
        return
      }
      showToast(requestError.message, 'error')
    } finally {
      setIsLoading(false)
    }
  }, [onLogout, selectedId, showToast, token])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  function handleFormChange(event) {
    const { name, value } = event.target
    setRepositoryForm((current) => ({ ...current, [name]: value }))
  }

  async function handleRepositorySubmit(event) {
    event.preventDefault()
    setIsSubmitting(true)
    try {
      const endpoint = isEditing && selectedRepository ? `/api/repositories/${selectedRepository.id}` : '/api/repositories'
      const method = isEditing ? 'PUT' : 'POST'
      const response = await fetchJson(endpoint, { method, body: JSON.stringify(repositoryForm) }, true, token)
      setRepositoryForm(EMPTY_REPOSITORY_FORM)
      setIsEditing(false)
      showToast(isEditing ? 'Repository updated successfully.' : 'Repository added successfully.')
      await loadDashboard(response.id)
      setActiveView('Repositories')
    } catch (requestError) {
      showToast(requestError.message, 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  function startEdit(repository) {
    setSelectedId(repository.id)
    setRepositoryForm({ owner: repository.owner, repositoryName: repository.repositoryName, alias: repository.alias || '', notes: repository.notes || '' })
    setIsEditing(true)
    setActiveView('Repositories')
  }

  function cancelEdit() {
    setRepositoryForm(EMPTY_REPOSITORY_FORM)
    setIsEditing(false)
  }

  async function handleDelete(repositoryId) {
    if (!window.confirm('Delete this repository from the dashboard?')) {
      return
    }
    setActiveAction(`delete-${repositoryId}`)
    try {
      await fetchJson(`/api/repositories/${repositoryId}`, { method: 'DELETE' }, false, token)
      showToast('Repository deleted.')
      if (selectedId === repositoryId) setSelectedId(null)
      await loadDashboard()
    } catch (requestError) {
      showToast(requestError.message, 'error')
    } finally {
      setActiveAction('')
    }
  }

  async function triggerRepositoryAction(action, repositoryId = selectedRepository?.id) {
    if (!repositoryId) return
    setActiveAction(`${action}-${repositoryId}`)
    try {
      const response = await fetchJson(`/api/repositories/${repositoryId}/${action}`, { method: 'POST' }, true, token)
      showToast(action === 'sync' ? 'Repository synced successfully.' : 'Repository analysis completed.')
      await loadDashboard(response.id)
      if (action === 'analyze') setActiveView('AI Insight')
    } catch (requestError) {
      showToast(requestError.message, 'error')
    } finally {
      setActiveAction('')
    }
  }

  async function handleAnalyzeFromUrl(event) {
    event.preventDefault()
    const parsed = parseRepositoryUrl(analysisUrl)
    if (!parsed) {
      setAnalysisError('Paste a valid GitHub repository URL.')
      return
    }
    setAnalysisError('')
    setIsAnalysisLoading(true)
    try {
      const existing = repositories.find((repository) => repository.owner.toLowerCase() === parsed.owner.toLowerCase() && repository.repositoryName.toLowerCase() === parsed.repositoryName.toLowerCase())
      if (existing) {
        setSelectedId(existing.id)
        await triggerRepositoryAction('analyze', existing.id)
      } else {
        const created = await fetchJson('/api/repositories', { method: 'POST', body: JSON.stringify({ owner: parsed.owner, repositoryName: parsed.repositoryName, alias: '', notes: '' }) }, true, token)
        showToast('Repository added successfully.')
        setSelectedId(created.id)
        await triggerRepositoryAction('analyze', created.id)
      }
      setAnalysisUrl('')
    } catch (requestError) {
      setAnalysisError(requestError.message)
    } finally {
      setIsAnalysisLoading(false)
    }
  }

  return (
    <div className="dashboard-shell top-layout">
      <header className="topbar floating-panel">
        <div className="topbar-brand">
          <p className="eyebrow">GitSense</p>
          <h1 className="brand-title">GitSense</h1>
          <p className="brand-subtitle">Smart understanding of Git repositories</p>
        </div>
        <nav className="top-nav">
          {NAV_ITEMS.map((item) => (
            <button key={item} type="button" className={activeView === item ? 'nav-item active' : 'nav-item'} onClick={() => setActiveView(item)}>
              {item}
            </button>
          ))}
        </nav>
        <div className="topbar-user">
          <ThemeToggle theme={theme} setTheme={setTheme} />
          <div className="user-card compact">
            <div className="avatar-badge">{initialsFromName(currentUser?.fullName)}</div>
            <div><strong>{currentUser?.fullName || 'User'}</strong><span>{currentUser?.email}</span></div>
          </div>
          <button type="button" className="ghost-button" onClick={onLogout}>Logout</button>
        </div>
      </header>

      <section className="workspace-header floating-panel">
        <div className="workspace-heading">
          <p className="eyebrow">{activeView}</p>
          <h2>{viewHeading(activeView)}</h2>
          <p className="workspace-subcopy">Pick a repository, inspect its activity, and run analysis without hunting through the page.</p>
        </div>
        <div className="header-actions">
          <div className="repo-select-wrap">
            <label htmlFor="repo-select">Selected repository</label>
            <select
              id="repo-select"
              value={selectedRepository?.id || ''}
              onChange={(event) => setSelectedId(Number(event.target.value))}
              disabled={!repositories.length}
            >
              {!repositories.length && <option value="">No repositories</option>}
              {repositories.map((repository) => (
                <option key={repository.id} value={repository.id}>
                  {repository.fullName}
                </option>
              ))}
            </select>
          </div>
          {selectedRepository && <button type="button" className="ghost-button" onClick={() => triggerRepositoryAction('sync')}>Sync</button>}
          {selectedRepository && <button type="button" className="primary-button" onClick={() => triggerRepositoryAction('analyze')}>Analyze</button>}
        </div>
      </section>

      <main className="main-area">
        {toast && <Toast message={toast.message} tone={toast.tone} />}
        {activeView === 'Overview' && <OverviewPage isLoading={isLoading} metrics={dashboardMetrics} languages={topLanguages} commitSeries={commitSeries} contributorSeries={contributorSeries} trendSeries={trendSeries} />}
        {activeView === 'Repositories' && <RepositoriesPage isLoading={isLoading} repositories={filteredRepositories} availableLanguages={availableLanguages} searchTerm={searchTerm} setSearchTerm={setSearchTerm} languageFilter={languageFilter} setLanguageFilter={setLanguageFilter} sortOrder={sortOrder} setSortOrder={setSortOrder} repositoryForm={repositoryForm} handleFormChange={handleFormChange} handleSubmit={handleRepositorySubmit} isSubmitting={isSubmitting} isEditing={isEditing} cancelEdit={cancelEdit} selectedId={selectedId} setSelectedId={setSelectedId} startEdit={startEdit} activeAction={activeAction} triggerRepositoryAction={triggerRepositoryAction} handleDelete={handleDelete} />}
        {activeView === 'Analytics' && <AnalyticsPage repository={selectedRepository} isLoading={isLoading} />}
        {activeView === 'AI Insight' && <AIInsightPage repository={selectedRepository} analysisUrl={analysisUrl} setAnalysisUrl={setAnalysisUrl} handleAnalyzeFromUrl={handleAnalyzeFromUrl} isAnalysisLoading={isAnalysisLoading} analysisError={analysisError} />}
      </main>
    </div>
  )
}
function OverviewPage({ isLoading, metrics, languages, commitSeries, contributorSeries, trendSeries }) {
  if (isLoading) return <SkeletonDashboard />
  return (
    <div className="page-stack">
      <section className="metric-grid">
        {metrics.map((metric) => <article key={metric.label} className="metric-card floating-panel"><span>{metric.label}</span><strong>{formatMetric(metric.value)}</strong></article>)}
      </section>
      <section className="widget-grid overview-grid">
        <ChartCard title="Language distribution"><LanguageDonutChart languages={languages} /></ChartCard>
        <ChartCard title="Commit frequency"><LineChart points={commitSeries} color="#3B82F6" /></ChartCard>
        <ChartCard title="Contributor activity"><BarChart items={contributorSeries} color="#22C55E" /></ChartCard>
        <ChartCard title="Development trend"><BarChart items={trendSeries} color="#F97316" compact /></ChartCard>
      </section>
    </div>
  )
}

function RepositoriesPage({ isLoading, repositories, availableLanguages, searchTerm, setSearchTerm, languageFilter, setLanguageFilter, sortOrder, setSortOrder, repositoryForm, handleFormChange, handleSubmit, isSubmitting, isEditing, cancelEdit, selectedId, setSelectedId, startEdit, activeAction, triggerRepositoryAction, handleDelete }) {
  return (
    <div className="page-stack repository-layout">
      <section className="floating-panel management-panel">
        <div className="panel-header-row"><h3>{isEditing ? 'Edit repository' : 'Add repository'}</h3>{isEditing && <button type="button" className="text-button" onClick={cancelEdit}>Cancel</button>}</div>
        <form className="repository-form" onSubmit={handleSubmit}>
          <label>Owner<input name="owner" value={repositoryForm.owner} onChange={handleFormChange} placeholder="facebook" required /></label>
          <label>Repository<input name="repositoryName" value={repositoryForm.repositoryName} onChange={handleFormChange} placeholder="react" required /></label>
          <label>Alias<input name="alias" value={repositoryForm.alias} onChange={handleFormChange} placeholder="Optional label" /></label>
          <label>Notes<textarea name="notes" value={repositoryForm.notes} onChange={handleFormChange} rows="4" placeholder="Context, ownership, review notes" /></label>
          <button type="submit" className="primary-button" disabled={isSubmitting}>{isSubmitting ? 'Saving...' : isEditing ? 'Save repository' : 'Add repository'}</button>
        </form>
      </section>

      <section className="floating-panel repository-table-panel">
        <div className="table-toolbar">
          <input className="search-input" value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Search repositories" />
          <select value={languageFilter} onChange={(event) => setLanguageFilter(event.target.value)}>{availableLanguages.map((language) => <option key={language} value={language}>{language}</option>)}</select>
          <select value={sortOrder} onChange={(event) => setSortOrder(event.target.value)}>
            <option value="activity">Sort by commit activity</option>
            <option value="recent">Sort by recent sync</option>
            <option value="stars">Sort by stars</option>
          </select>
        </div>
        {isLoading ? <SkeletonTable /> : (
          <div className="repository-table-wrap">
            <table className="repository-table">
              <thead><tr><th>Repository Name</th><th>Owner</th><th>Primary Language</th><th>Last Commit Date</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>
                {repositories.map((repository) => (
                  <tr key={repository.id} className={selectedId === repository.id ? 'selected-row' : ''} onClick={() => setSelectedId(repository.id)}>
                    <td><div className="repo-identity"><img src={ownerAvatar(repository.owner)} alt={repository.owner} className="repo-avatar" /><div><strong>{repository.repositoryName}</strong><span>{repository.alias || repository.fullName}</span></div></div></td>
                    <td>{repository.owner}</td>
                    <td><LanguageBadge language={repository.primaryLanguage} /></td>
                    <td>{formatDate(repository.lastSyncedAt)}</td>
                    <td><span className="status-pill">{repository.healthAssessment || 'Pending'}</span></td>
                    <td>
                      <div className="table-actions" onClick={(event) => event.stopPropagation()}>
                        <button type="button" className="icon-action" onClick={() => setSelectedId(repository.id)}>View Analytics</button>
                        <button type="button" className="icon-action" onClick={() => triggerRepositoryAction('analyze', repository.id)} disabled={activeAction === `analyze-${repository.id}`}>Analyze</button>
                        <button type="button" className="icon-action" onClick={() => startEdit(repository)}>Edit</button>
                        <button type="button" className="icon-action danger" onClick={() => handleDelete(repository.id)} disabled={activeAction === `delete-${repository.id}`}>Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

function AnalyticsPage({ repository, isLoading }) {
  if (isLoading) return <SkeletonAnalytics />
  if (!repository) return <EmptyState title="No repository selected" description="Choose a repository from the management table to inspect analytics." />
  const commitTimeline = repository.commitTimeline || []
  const contributors = (repository.contributors || []).map((item) => ({ label: item.login, value: item.contributions }))
  const languages = repository.languages || []

  return (
    <div className="page-stack">
      <section className="analytics-hero floating-panel">
        <div className="analytics-repo-header"><img src={ownerAvatar(repository.owner)} alt={repository.owner} className="repo-avatar large" /><div><h3>{repository.fullName}</h3><p>{repository.description || 'No description available.'}</p></div></div>
        <div className="analytics-meta-strip"><MetricTile label="Owner" value={repository.owner} /><MetricTile label="Stars" value={repository.stars || 0} /><MetricTile label="Forks" value={repository.forks || 0} /><MetricTile label="Watchers" value={repository.watchers || 0} /></div>
      </section>
      <section className="widget-grid analytics-grid">
        <ChartCard title="Language distribution chart"><LanguageDonutChart languages={languages} /></ChartCard>
        <ChartCard title="Commit timeline"><LineChart points={commitTimeline.map((point) => ({ label: point.label, value: point.commits }))} color="#3B82F6" /></ChartCard>
        <ChartCard title="Contributor activity"><BarChart items={contributors} color="#22C55E" /></ChartCard>
        <ChartCard title="File structure overview"><StructureOverview repository={repository} /></ChartCard>
      </section>
    </div>
  )
}

function AIInsightPage({ repository, analysisUrl, setAnalysisUrl, handleAnalyzeFromUrl, isAnalysisLoading, analysisError }) {
  return (
    <div className="page-stack ai-page">
      <section className="floating-panel ai-control-panel">
        <div><h3>Analyze Repository</h3><p>Paste a GitHub repository URL to generate AI-powered insight cards.</p></div>
        <form className="ai-form" onSubmit={handleAnalyzeFromUrl}><input value={analysisUrl} onChange={(event) => setAnalysisUrl(event.target.value)} placeholder="https://github.com/owner/repository" /><button type="submit" className="primary-button" disabled={isAnalysisLoading}>Analyze Repository</button></form>
        {isAnalysisLoading && <LoaderCard message="Analyzing repository structure and generating insights..." />}
        {analysisError && <p className="inline-message error">{analysisError}</p>}
      </section>
      {!repository ? <EmptyState title="No analyzed repository yet" description="Select a tracked repository or paste a GitHub URL above to start analysis." /> : (
        <section className="ai-insight-grid">
          <InsightAccordion title="Project Purpose" content={repository.insight?.purpose || 'Pending insight.'} defaultOpen />
          <InsightAccordion title="Technologies Used" content={(repository.insight?.technologies || []).join(', ') || 'Pending insight.'} defaultOpen />
          <InsightAccordion title="Codebase Complexity" content={repository.insight?.complexity || 'Pending insight.'} defaultOpen />
          <InsightAccordion title="Architecture Overview" content={repository.insight?.overview || 'Pending insight.'} defaultOpen />
          <InsightAccordion title="Suggested Improvements" list={repository.insight?.improvements || []} defaultOpen />
        </section>
      )}
    </div>
  )
}

function ThemeToggle({ theme, setTheme, compact = false }) {
  return <button type="button" className={compact ? 'theme-toggle compact' : 'theme-toggle'} onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>{theme === 'dark' ? 'Light mode' : 'Dark mode'}</button>
}

function ChartCard({ title, children }) {
  return <article className="chart-card floating-panel"><div className="card-header"><h3>{title}</h3></div>{children}</article>
}

function LanguageDonutChart({ languages }) {
  const chartLanguages = languages.length ? languages.slice(0, 6) : [{ name: 'No data', percentage: 100 }]
  const segments = chartLanguages.map((language, index) => ({ ...language, color: languageColor(language.name, index) }))
  return (
    <div className="donut-layout">
      <div className="donut-chart" style={{ backgroundImage: buildConicGradient(segments) }}><div className="donut-hole"><span>{segments.length}</span><small>languages</small></div></div>
      <div className="legend-list">{segments.map((language) => <div key={language.name} className="legend-item" title={`${language.name}: ${language.percentage}%`}><span className="legend-dot" style={{ backgroundColor: language.color }} /><span>{language.name}</span><strong>{language.percentage}%</strong></div>)}</div>
    </div>
  )
}

function LineChart({ points, color }) {
  if (!points.length) return <EmptyMiniState message="No commit data yet." />
  const width = 420
  const height = 220
  const max = Math.max(...points.map((point) => point.value), 1)
  const coordinates = points.map((point, index) => ({ ...point, x: (index / Math.max(points.length - 1, 1)) * (width - 40) + 20, y: height - ((point.value / max) * 150 + 30) }))
  const polyline = coordinates.map((point) => `${point.x},${point.y}`).join(' ')
  return (
    <div className="line-chart-wrap">
      <svg viewBox={`0 0 ${width} ${height}`} className="line-chart" preserveAspectRatio="none">
        <polyline className="line-grid" points={`20,180 ${width - 20},180`} />
        <polyline className="line-grid" points={`20,100 ${width - 20},100`} />
        <polyline fill="none" stroke={color} strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" points={polyline} />
        {coordinates.map((point) => <circle key={point.label} cx={point.x} cy={point.y} r="6" fill={color}><title>{`${point.label}: ${point.value}`}</title></circle>)}
      </svg>
      <div className="chart-axis-labels">{points.map((point) => <span key={point.label}>{point.label}</span>)}</div>
    </div>
  )
}

function BarChart({ items, color, compact = false }) {
  if (!items.length) return <EmptyMiniState message="No activity data yet." />
  const max = Math.max(...items.map((item) => item.value), 1)
  return <div className={compact ? 'bar-chart compact' : 'bar-chart'}>{items.map((item) => <div key={item.label} className="bar-row" title={`${item.label}: ${item.value}`}><div className="bar-copy"><span>{item.label}</span><strong>{item.value}</strong></div><div className="bar-track"><div className="bar-fill" style={{ width: `${(item.value / max) * 100}%`, background: color }} /></div></div>)}</div>
}
function StructureOverview({ repository }) {
  return <div className="structure-grid"><div><span>Default branch</span><strong>{repository.defaultBranch || 'Unknown'}</strong></div><div><span>Visibility</span><strong>{repository.visibility || 'Unknown'}</strong></div><div><span>Topics</span><strong>{(repository.topics || []).join(', ') || 'Not available'}</strong></div><div><span>AI technologies</span><strong>{(repository.insight?.technologies || []).join(', ') || 'Not available'}</strong></div></div>
}

function InsightAccordion({ title, content, list = [], defaultOpen = false }) {
  return <details className="insight-card floating-panel" open={defaultOpen}><summary><span>{title}</span><strong>Expand</strong></summary>{list.length > 0 ? <ul className="simple-list spaced">{list.map((item) => <li key={item}>{item}</li>)}</ul> : <p>{content}</p>}</details>
}

function LoaderCard({ message }) {
  return <div className="loader-card"><div className="loader-orbit" /><p>{message}</p></div>
}

function Toast({ message, tone }) {
  return <div className={`toast ${tone}`}>{message}</div>
}

function EmptyState({ title, description }) {
  return <div className="empty-state floating-panel large"><h3>{title}</h3><p>{description}</p></div>
}

function EmptyMiniState({ message }) {
  return <div className="empty-mini-state">{message}</div>
}

function SkeletonDashboard() {
  return <div className="page-stack"><section className="metric-grid">{Array.from({ length: 4 }).map((_, index) => <div key={index} className="skeleton metric-skeleton floating-panel" />)}</section><section className="widget-grid overview-grid">{Array.from({ length: 4 }).map((_, index) => <div key={index} className="skeleton chart-skeleton floating-panel" />)}</section></div>
}

function SkeletonTable() {
  return <div className="table-skeleton">{Array.from({ length: 6 }).map((_, index) => <div key={index} className="skeleton row-skeleton" />)}</div>
}

function SkeletonAnalytics() {
  return <div className="page-stack"><div className="skeleton analytics-hero-skeleton floating-panel" /><section className="widget-grid analytics-grid">{Array.from({ length: 4 }).map((_, index) => <div key={index} className="skeleton chart-skeleton floating-panel" />)}</section></div>
}

function MetricTile({ label, value }) {
  return <div className="metric-tile"><span>{label}</span><strong>{value}</strong></div>
}

function LanguageBadge({ language }) {
  const meta = LANGUAGE_META[language] || { icon: language ? language.slice(0, 2).toUpperCase() : '--', color: '#64748B' }
  return <span className="language-badge" style={{ '--badge-color': meta.color }}><span className="language-icon">{meta.icon}</span>{language || 'Unknown'}</span>
}

function formatDate(value) {
  if (!value) return 'Not available'
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' }).format(new Date(value))
}

function formatMetric(value) {
  return new Intl.NumberFormat('en-US', { notation: value > 9999 ? 'compact' : 'standard' }).format(Number(value || 0))
}

function ownerAvatar(owner) {
  return `https://github.com/${owner}.png?size=96`
}

function initialsFromName(name = '') {
  return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'U'
}

function viewHeading(activeView) {
  if (activeView === 'Overview') return 'Repository signals overview'
  if (activeView === 'Repositories') return 'Repository management'
  if (activeView === 'Analytics') return 'Detailed repository analytics'
  return 'AI codebase insight'
}

function aggregateLanguages(repositories) {
  const totals = new Map()
  repositories.forEach((repository) => {
    ;(repository.languages || []).forEach((language) => {
      totals.set(language.name, (totals.get(language.name) || 0) + language.percentage)
    })
  })
  const sum = Array.from(totals.values()).reduce((acc, value) => acc + value, 0) || 1
  return Array.from(totals.entries()).map(([name, value]) => ({ name, percentage: Number(((value / sum) * 100).toFixed(1)) })).sort((left, right) => right.percentage - left.percentage).slice(0, 6)
}

function aggregateCommitTimeline(repositories) {
  const buckets = new Map()
  repositories.forEach((repository) => {
    ;(repository.commitTimeline || []).forEach((point) => {
      buckets.set(point.label, (buckets.get(point.label) || 0) + point.commits)
    })
  })
  return Array.from(buckets.entries()).map(([label, value]) => ({ label, value }))
}

function aggregateContributors(repositories) {
  const buckets = new Map()
  repositories.forEach((repository) => {
    ;(repository.contributors || []).forEach((contributor) => {
      buckets.set(contributor.login, (buckets.get(contributor.login) || 0) + contributor.contributions)
    })
  })
  return Array.from(buckets.entries()).map(([label, value]) => ({ label, value })).sort((left, right) => right.value - left.value).slice(0, 6)
}

function buildConicGradient(segments) {
  let current = 0
  const stops = segments.map((segment) => {
    const start = current
    const end = current + segment.percentage
    current = end
    return `${segment.color} ${start}% ${end}%`
  })
  return `conic-gradient(${stops.join(', ')})`
}

function languageColor(language, index) {
  return LANGUAGE_META[language]?.color || ['#3B82F6', '#22C55E', '#F97316', '#A855F7', '#EAB308', '#06B6D4'][index % 6]
}

function parseRepositoryUrl(url) {
  try {
    const normalized = url.trim()
    const match = normalized.match(/github\.com\/(.+?)\/(.+?)(?:\.git|\/)?$/i)
    if (!match) return null
    return { owner: match[1], repositoryName: match[2] }
  } catch {
    return null
  }
}

async function fetchJson(path, options = {}, expectJson = true, token = '') {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
    ...options,
  })
  if (!response.ok) {
    const errorPayload = await response.json().catch(async () => ({ message: await response.text() }))
    const error = new Error(errorPayload?.message || 'Request failed.')
    error.status = response.status
    throw error
  }
  if (!expectJson || response.status === 204) return null
  return response.json()
}

export default App
