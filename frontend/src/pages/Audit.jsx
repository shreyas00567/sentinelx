import { useState } from 'react'
import { api, useFetch, Pagination, fmtTime } from '../components/ui'

export default function Audit() {
  const [page, setPage] = useState(0)
  const { data, error, loading } = useFetch(
    () => api('/api/audit', { params: { page, size: 50 } }),
    [page]
  )

  if (error) return <div className="error-box">{String(error)}</div>

  return (
    <div className="card">
      <h3>Audit Trail (admin only)</h3>
      {loading ? <div className="loading">Loading…</div> : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Time</th><th>Actor</th><th>Action</th><th>Details</th><th>IP</th></tr>
              </thead>
              <tbody>
                {(data?.content || []).map(a => (
                  <tr key={a.id}>
                    <td className="dim mono">{fmtTime(a.timestamp)}</td>
                    <td>{a.username}</td>
                    <td>{a.action}</td>
                    <td className="dim">{a.details}</td>
                    <td className="mono dim">{a.ipAddress}</td>
                  </tr>
                ))}
                {!data?.content?.length && <tr><td colSpan={5} className="empty">No audit entries.</td></tr>}
              </tbody>
            </table>
          </div>
          <Pagination page={page} size={data?.size || 50} total={data?.totalElements || 0} onPage={setPage} />
        </>
      )}
    </div>
  )
}
